# Frontend Integration Guide - Apigee Product Import

## Required Fields for Connection Setup

### 1. **Organization** (org) - REQUIRED
**Where to get it:**
- Go to Google Cloud Console
- Navigate to Apigee section
- Find "Organization" in the overview
- Example: `aforo-aadhaar-477607`

**UI Field:**
- Label: "Apigee Organization"
- Type: Text input
- Placeholder: "Enter your Apigee organization name"
- Validation: Required

---

### 2. **Environment** (envs) - REQUIRED
**Where to get it:**
- In Apigee Console → Admin → Environments
- Common values: `eval`, `prod`, `test`, `dev`
- Example: `eval`

**UI Field:**
- Label: "Environment"
- Type: Text input or Dropdown
- Placeholder: "e.g., eval, prod, test"
- Validation: Required

---

### 3. **Analytics Mode** (analyticsMode) - REQUIRED
**Default value:** `STANDARD`

**Options:**
- `STANDARD` (recommended)
- `CUSTOM`
- `NONE`

**UI Field:**
- Label: "Analytics Mode"
- Type: Dropdown
- Default: "STANDARD"
- Validation: Required

---

### 4. **Service Account JSON** (serviceAccountJson) - REQUIRED

**Where to get it:**

**Step 1:** Go to Google Cloud Console
- Navigate to: IAM & Admin → Service Accounts
- URL: https://console.cloud.google.com/iam-admin/serviceaccounts

**Step 2:** Create or select service account
- If creating new: Click "Create Service Account"
- Name: e.g., "apigee-integration"
- Grant role: "Apigee Admin" or "Apigee Organization Admin"

**Step 3:** Create JSON key
- Click on the service account
- Go to "Keys" tab
- Click "Add Key" → "Create New Key"
- Select "JSON" format
- Click "Create" - file will download

**Step 4:** Upload in UI
- User uploads the downloaded JSON file
- Frontend reads file content as string
- Send the entire JSON content in the request

**UI Implementation:**
```javascript
// File upload handler
const handleFileUpload = (event) => {
  const file = event.target.files[0];
  const reader = new FileReader();
  
  reader.onload = (e) => {
    const jsonContent = e.target.result;
    setServiceAccountJson(jsonContent); // Store as string
  };
  
  reader.readAsText(file);
};
```

**UI Field:**
- Label: "Service Account JSON"
- Type: File upload (accept=".json")
- Help text: "Upload the service account JSON key file from Google Cloud"
- Validation: Required, must be valid JSON

---

### 5. **HMAC Secret** (hmacSecret) - OPTIONAL

**What is it:**
- Used for webhook signature validation
- System will auto-generate if not provided

**Where to get it:**
- User can provide their own secret (any random string)
- OR leave empty - system generates automatically

**UI Field:**
- Label: "HMAC Secret (Optional)"
- Type: Text input (password type)
- Placeholder: "Leave empty to auto-generate"
- Help text: "Used for webhook validation. Auto-generated if not provided."
- Validation: Optional

---

## API Request Format

### For Production/AWS (Recommended):
```json
{
  "org": "aforo-aadhaar-477607",
  "envs": "eval",
  "analyticsMode": "STANDARD",
  "serviceAccountJson": "{\"type\":\"service_account\",\"project_id\":\"aforo-aadhaar-477607\",\"private_key_id\":\"9d73f68217f7...\",\"private_key\":\"-----BEGIN PRIVATE KEY-----\\n...\\n-----END PRIVATE KEY-----\\n\",\"client_email\":\"apigee-integration@aforo-aadhaar-477607.iam.gserviceaccount.com\",...}"
}
```

### For Local Development (Optional):
```json
{
  "org": "aforo-aadhaar-477607",
  "envs": "eval",
  "analyticsMode": "STANDARD",
  "saJsonPath": "/path/to/service-account.json"
}
```

---

## UI Flow

### Screen 1: Connection Setup

```
┌─────────────────────────────────────────────┐
│  Connect to Apigee                          │
├─────────────────────────────────────────────┤
│                                             │
│  Organization *                             │
│  ┌─────────────────────────────────────┐   │
│  │ aforo-aadhaar-477607                │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  Environment *                              │
│  ┌─────────────────────────────────────┐   │
│  │ eval                          ▼     │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  Analytics Mode *                           │
│  ┌─────────────────────────────────────┐   │
│  │ STANDARD                      ▼     │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  Service Account JSON *                     │
│  ┌─────────────────────────────────────┐   │
│  │ [📁 Upload JSON File]               │   │
│  └─────────────────────────────────────┘   │
│  ℹ️ Download from Google Cloud Console     │
│                                             │
│  HMAC Secret (Optional)                     │
│  ┌─────────────────────────────────────┐   │
│  │ ••••••••••••••••                    │   │
│  └─────────────────────────────────────┘   │
│  ℹ️ Leave empty to auto-generate            │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │        Test Connection              │   │
│  └─────────────────────────────────────┘   │
│                                             │
└─────────────────────────────────────────────┘
```

---

## Help Text / Tooltips

### Organization
"Your Apigee organization name from Google Cloud Console → Apigee"

### Environment
"The Apigee environment to connect to (e.g., eval, prod, test)"

### Analytics Mode
"Analytics collection mode. Use STANDARD for most cases."

### Service Account JSON
"Upload the JSON key file downloaded from Google Cloud Console → IAM & Admin → Service Accounts"

**Detailed Instructions Link:**
- Provide a "How to get this?" link
- Opens modal with step-by-step screenshots

### HMAC Secret
"Optional secret for webhook validation. System will generate one if not provided."

---

## Validation Rules

1. **Organization**: Required, alphanumeric with hyphens
2. **Environment**: Required, non-empty string
3. **Analytics Mode**: Required, must be one of: STANDARD, CUSTOM, NONE
4. **Service Account JSON**: 
   - Required
   - Must be valid JSON
   - Must contain required fields: type, project_id, private_key, client_email
5. **HMAC Secret**: Optional, any string

---

## Error Handling

### Common Errors:

**Invalid Service Account JSON:**
```json
{
  "connected": false,
  "message": "Invalid service account JSON format"
}
```
**Solution:** Verify the uploaded file is the correct JSON key from Google Cloud

**Permission Denied:**
```json
{
  "connected": false,
  "message": "Service account lacks required permissions"
}
```
**Solution:** Grant "Apigee Organization Admin" role to the service account

**Organization Not Found:**
```json
{
  "connected": false,
  "message": "Failed to connect to Apigee org: xyz"
}
```
**Solution:** Verify the organization name is correct

---

## Testing

### Test Connection Button
- Shows loading spinner
- Makes POST request to `/api/integrations/apigee/connections`
- On success: Show ✅ "Connected successfully"
- On failure: Show ❌ error message
- Enable "Next" button only after successful connection

---

## Summary

**Minimum Required Fields:**
1. Organization (from Apigee Console)
2. Environment (from Apigee Console)
3. Analytics Mode (default: STANDARD)
4. Service Account JSON (upload file from Google Cloud)

**Optional:**
- HMAC Secret (auto-generated if not provided)

**Total user inputs:** 4 fields (3 text + 1 file upload)
