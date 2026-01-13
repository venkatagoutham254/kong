# Kong Konnect - Pre-Testing Setup Checklist

## What You Need to Do in Kong Konnect Dashboard

### ✅ Step 1: Verify Your Control Plane Exists
1. Go to: https://cloud.konghq.com/
2. Navigate to **Gateway Manager** (left sidebar)
3. You should see at least one **Control Plane**
   - If you don't have one, click **New Control Plane** and create one
   - Name it something like "aforo-dev" or "production"

**Why needed**: The API needs a control plane to fetch API products from.

---

### ✅ Step 2: Create Some API Products (For Testing)
1. In Konnect dashboard, go to **API Products** (left sidebar)
2. Click **New API Product**
3. Create at least 2-3 test API products:

**Example API Product 1:**
- **Name**: Payment API
- **Description**: Payment processing service
- **Status**: Published

**Example API Product 2:**
- **Name**: User Management API
- **Description**: User authentication and management
- **Status**: Published

**Example API Product 3:**
- **Name**: Notification API
- **Description**: Email and SMS notifications
- **Status**: Published

**Why needed**: We need some products to test import/sync functionality.

---

### ✅ Step 3: Verify Your PAT Token Has Permissions
Your token: `kpat_bIrfala27dVuTxFwwR4urp7BvTvQGtxmybs2eUfcfzUsUedhB`

**Check permissions:**
1. Go to **Personal Access Tokens** in your profile
2. Find your token
3. Verify it has these permissions:
   - ✅ Read Control Planes
   - ✅ Read API Products
   - ✅ (Optional) Write API Products

**Note**: If you just created the token, it should have all necessary permissions by default.

---

### ✅ Step 4: Note Your Control Plane ID
1. In **Gateway Manager**, click on your control plane
2. Copy the **Control Plane ID** from the URL or details page
   - Format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
   - Example: `22ef7dda-4ad5-45c4-8079-001ac07ddcad`

**Optional**: If you don't provide this, the system will auto-select the first control plane.

---

## Summary - What Should Exist in Konnect

Before testing, verify you have:

- ✅ **Active Konnect account** (IN region)
- ✅ **At least 1 Control Plane** created
- ✅ **2-3 API Products** created (for testing import)
- ✅ **PAT Token** with read permissions (you already have this)

---

## Region Confirmation

Your region: **IN (India)**
Base URL: `https://in.api.konghq.com`

⚠️ **Note**: Make sure you're using the India region URL, not US/EU/AU.

---

## Quick Verification Test (Optional)

You can manually test your token works:

```bash
curl -X GET "https://in.api.konghq.com/v2/control-planes" \
  -H "Authorization: Bearer kpat_bIrfala27dVuTxFwwR4urp7BvTvQGtxmybs2eUfcfzUsUedhB"
```

**Expected**: Should return JSON with your control planes.

---

## Next Steps

Once you confirm the above exists in Konnect:
1. I'll generate the encryption key
2. Update your application.properties
3. Start the application
4. Run all test commands

**Ready?** Let me know if you have:
- ✅ Control plane created
- ✅ API products created (how many?)
- ✅ Control plane ID (optional)

Then we can proceed with testing!
