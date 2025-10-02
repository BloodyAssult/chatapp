package org.danial.chatapp.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.danial.chatapp.model.ChatMessageModel;

import java.io.Closeable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * سرویس برای فراخوانی AvalAI / OpenAI با کتابخانه رسمی openai-java
 * اصلاحات:
 *  - استفاده از builder مناسب برای ChatCompletionCreateParams (addUserMessage)
 *  - فراخوانی صحیح: client.chat().completions().create(...)
 *  - استخراج محتوا از پاسخ به صورت ایمن و جمع‌آوری رشته‌ها
 */
public class AvalAIService {

    private static final Logger LOG = Logger.getLogger(AvalAIService.class.getName());
    private final OpenAIClient client;

    public AvalAIService() {
        String apiKey = System.getenv("AVALAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AVALAI_API_KEY environment variable is not set.");
        }

        String baseUrl = System.getenv().getOrDefault("AVALAI_BASE_URL", "https://api.avalai.ir/v1");

        // ساخت کلاینت با OkHttp و تنظیم API key + baseUrl
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * ارسال prompt به AvalAI / OpenAI و دریافت پاسخ به صورت ChatMessageModel
     *
     * @param prompt متن ورودی کاربر
     * @param model  مدل (مثلاً ChatModel.GPT_4_1.name() یا نام رشته‌ای مدلی که سرور پشتیبانی می‌کند)
     */
    public ChatMessageModel sendToAvalAI(String prompt, String model) {
        if (prompt == null || prompt.isBlank()) throw new IllegalArgumentException("prompt required");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model required");

        try {
            // اگر می‌خواهید از enum‌های SDK استفاده کنید میتوانید ChatModel را بفرستید،
            // اینجا از رشته مدل استفاده می‌کنیم تا با endpointهای سفارشی/آوال‌ای سازگار بماند.
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .addUserMessage(prompt)         // اضافه کردن پیام کاربر
                    .model(model)                   // نام مدل (رشته)
                    .maxCompletionTokens(4000)      // حداکثر توکن پاسخ (تنظیم متناسب با نیاز)
                    .temperature(0.7)               // خلاقیت
                    .build();

            // فراخوانی API (روش درست: chat().completions().create)
            ChatCompletion response = client.chat().completions().create(params);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                LOG.warning("No choices returned from API.");
                return new ChatMessageModel("AI", "هیچ پاسخی از سرور دریافت نشد.");
            }

            // استخراج متن: هر choice ممکن است یک message داشته باشد که خودش یک یا چند تکه متن (content) دارد.
            String text = response.choices().stream()
                    .map(choice -> choice.message().content())
                    .filter(Objects::nonNull)
                    .flatMap(list -> list.stream())                               // List<String> -> stream of String
                    .collect(Collectors.joining("\n"));                     // join به یک رشته واحد

            if (text.isBlank()) text = "پاسخ خالی از سرور دریافت شد.";

            return new ChatMessageModel("AI", text);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error calling AvalAI/OpenAI: " + e.getMessage(), e);
            return new ChatMessageModel("AI", "خطا در ارتباط با سرویس: " + e.getMessage());
        }
    }

    public void shutdown() {
        try {
            // کلاینت openai-java معمولا Closeable است؛ اگر نیست این فراخوانی را حذف کنید.
            if (this.client instanceof Closeable) {
                ((Closeable) this.client).close();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error while closing client: " + e.getMessage(), e);
        }
    }
}
