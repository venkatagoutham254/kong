package aforo.kong.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public class ApigeeImportRequest {
    
    @JsonProperty("selectedProducts")
    @Schema(description = "List of products to import", required = true)
    private List<ApigeeProduct> selectedProducts;
    
    public List<ApigeeProduct> getSelectedProducts() {
        return selectedProducts;
    }
    
    public void setSelectedProducts(List<ApigeeProduct> selectedProducts) {
        this.selectedProducts = selectedProducts;
    }
    
    public static class ApigeeProduct {
        @JsonProperty("productName")
        @Schema(description = "Product name (REQUIRED - only field needed)", required = true, example = "testing 4")
        private String productName;
        
        @JsonProperty("displayName")
        @Schema(description = "Display name (OPTIONAL - auto-assigned from productName if not provided)", required = false, example = "")
        private String displayName;
        
        @JsonProperty("productType")
        @Schema(description = "Product type (OPTIONAL - auto-assigned as 'API' if not provided)", required = false, example = "")
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
