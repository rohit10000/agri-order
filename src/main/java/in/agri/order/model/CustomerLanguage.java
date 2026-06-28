package in.agri.order.model;

public enum CustomerLanguage {
    HINDI("hindi"),
    ENGLISH("english");

    private final String code;

    CustomerLanguage(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CustomerLanguage fromCode(String code) {
        for (CustomerLanguage lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) {
                return lang;
            }
        }
        return HINDI; // default
    }
}
