package in.agri.order.model;

public enum ProductItem {
    WHEAT("Wheat", "गेहूं"),
    RICE("Rice", "चावल"),
    ATTA("Wheat Flour", "आटा");

    private final String englishName;
    private final String hindiName;

    ProductItem(String englishName, String hindiName) {
        this.englishName = englishName;
        this.hindiName = hindiName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getHindiName() {
        return hindiName;
    }

    public String getLocalizedName(CustomerLanguage language) {
        return language == CustomerLanguage.ENGLISH ? englishName : hindiName;
    }
}
