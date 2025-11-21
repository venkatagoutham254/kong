package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ApigeeImportRequest {
    
    @JsonProperty("selectedProducts")
    private List<ApigeeProduct> selectedProducts;
    
    public List<ApigeeProduct> getSelectedProducts() {
        return selectedProducts;
    }
    
    public void setSelectedProducts(List<ApigeeProduct> selectedProducts) {
        this.selectedProducts = selectedProducts;
    }
    
    public static class ApigeeProduct {
        @JsonProperty("productName")
        private String productName;
        
        @JsonProperty("displayName")
        private String displayName;
        
        @JsonProperty("productType")
        private String productType;
        
        public String getProductName() {
            return productName;
        }
        
        public void setProductName(String productName) {
            this.productName = productName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
        
        public String getProductType() {
            return productType;
        }
        
        public void setProductType(String productType) {
            this.productType = productType;
        }
    }
}
