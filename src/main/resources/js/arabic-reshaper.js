var ArabicReshaper = (function() {
    'use strict';

    var arabicLetters = {
        0x0621: [0xFE80],               // HAMZA
        0x0622: [0xFE81, 0xFE82],       // ALEF WITH MADDA ABOVE
        0x0623: [0xFE83, 0xFE84],       // ALEF WITH HAMZA ABOVE
        0x0624: [0xFE85, 0xFE86],       // WAW WITH HAMZA ABOVE
        0x0625: [0xFE87, 0xFE88],       // ALEF WITH HAMZA BELOW
        0x0626: [0xFE89, 0xFE8A, 0xFE8B, 0xFE8C], // YEH WITH HAMZA ABOVE
        0x0627: [0xFE8D, 0xFE8E],       // ALEF
        0x0628: [0xFE8F, 0xFE90, 0xFE91, 0xFE92], // BEH
        0x0629: [0xFE93, 0xFE94],       // TEH MARBUTA
        0x062A: [0xFE95, 0xFE96, 0xFE97, 0xFE98], // TEH
        0x062B: [0xFE99, 0xFE9A, 0xFE9B, 0xFE9C], // THEH
        0x062C: [0xFE9D, 0xFE9E, 0xFE9F, 0xFEA0], // JEEM
        0x062D: [0xFEA1, 0xFEA2, 0xFEA3, 0xFEA4], // HAH
        0x062E: [0xFEA5, 0xFEA6, 0xFEA7, 0xFEA8], // KHAH
        0x062F: [0xFEA9, 0xFEAA],       // DAL
        0x0630: [0xFEAB, 0xFEAC],       // THAL
        0x0631: [0xFEAD, 0xFEAE],       // REH
        0x0632: [0xFEAF, 0xFEB0],       // ZAIN
        0x0633: [0xFEB1, 0xFEB2, 0xFEB3, 0xFEB4], // SEEN
        0x0634: [0xFEB5, 0xFEB6, 0xFEB7, 0xFEB8], // SHEEN
        0x0635: [0xFEB9, 0xFEBA, 0xFEBB, 0xFEBC], // SAD
        0x0636: [0xFEBD, 0xFEBE, 0xFEBF, 0xFEC0], // DAD
        0x0637: [0xFEC1, 0xFEC2, 0xFEC3, 0xFEC4], // TAH
        0x0638: [0xFEC5, 0xFEC6, 0xFEC7, 0xFEC8], // ZAH
        0x0639: [0xFEC9, 0xFECA, 0xFECB, 0xFECC], // AIN
        0x063A: [0xFECD, 0xFECE, 0xFECF, 0xFED0], // GHAIN
        0x0640: [0x0640, 0x0640, 0x0640, 0x0640], // TATWEEL
        0x0641: [0xFED1, 0xFED2, 0xFED3, 0xFED4], // FEH
        0x0642: [0xFED5, 0xFED6, 0xFED7, 0xFED8], // QAF
        0x0643: [0xFED9, 0xFEDA, 0xFEDB, 0xFEDC], // KAF
        0x0644: [0xFEDD, 0xFEDE, 0xFEDF, 0xFEE0], // LAM
        0x0645: [0xFEE1, 0xFEE2, 0xFEE3, 0xFEE4], // MEEM
        0x0646: [0xFEE5, 0xFEE6, 0xFEE7, 0xFEE8], // NOON
        0x0647: [0xFEE9, 0xFEEA, 0xFEEB, 0xFEEC], // HEH
        0x0648: [0xFEED, 0xFEEE],       // WAW
        0x0649: [0xFEEF, 0xFEF0],       // ALEF MAKSURA
        0x064A: [0xFEF1, 0xFEF2, 0xFEF3, 0xFEF4], // YEH
        // Persian letters
        0x067E: [0xFB56, 0xFB57, 0xFB58, 0xFB59], // PEH
        0x0686: [0xFB7A, 0xFB7B, 0xFB7C, 0xFB7D], // TCHEH
        0x0698: [0xFB8A, 0xFB8B],       // JEH
        0x06A9: [0xFB8E, 0xFB8F, 0xFB90, 0xFB91], // KEHEH
        0x06AF: [0xFB92, 0xFB93, 0xFB94, 0xFB95], // GAF
        0x06CC: [0xFBFC, 0xFBFD, 0xFBFE, 0xFBFF]  // FARSI YEH
    };

    var nonConnecting = [
        0x0621, 0x0622, 0x0623, 0x0624, 0x0625, 0x0627,
        0x062F, 0x0630, 0x0631, 0x0632, 0x0648, 0x0649,
        0x0698, 0x0629
    ];

    function isArabic(c) {
        var code = c.charCodeAt(0);
        return code >= 0x0600 && code <= 0x06FF;
    }

    function canConnect(c) {
        var code = c.charCodeAt(0);
        return nonConnecting.indexOf(code) === -1;
    }

    function getForm(char, prev, next) {
        var code = char.charCodeAt(0);
        var forms = arabicLetters[code];
        
        if (!forms) return char;
        
        var hasPrev = prev && isArabic(prev) && canConnect(prev);
        var hasNext = next && isArabic(next) && canConnect(char);
        
        if (forms.length === 2) {
            return String.fromCharCode(hasPrev ? forms[1] : forms[0]);
        } else if (forms.length === 4) {
            if (hasPrev && hasNext) return String.fromCharCode(forms[2]); // medial
            if (hasPrev) return String.fromCharCode(forms[3]); // final
            if (hasNext) return String.fromCharCode(forms[1]); // initial
            return String.fromCharCode(forms[0]); // isolated
        }
        
        return char;
    }

    function reshapeArabic(text) {
        if (!text) return '';
        
        var result = '';
        for (var i = 0; i < text.length; i++) {
            var current = text[i];
            var prev = i > 0 ? text[i - 1] : null;
            var next = i < text.length - 1 ? text[i + 1] : null;
            
            if (isArabic(current)) {
                result += getForm(current, prev, next);
            } else {
                result += current;
            }
        }
        
        return result;
    }

    return {
        reshape: reshapeArabic
    };
})();