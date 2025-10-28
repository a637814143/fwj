<template>
  <form class="house-form" @submit.prevent="submitForm">
    <header class="form-header">
      <div>
        <h2>{{ isEditing ? 'Edit listing' : 'Add listing' }}</h2>
        <p v-if="!canManage" class="notice">
          This account can browse listings only. Switch to a seller or admin account to manage listings.
        </p>
      </div>
      <div v-if="isEditing" class="status-indicator" :class="statusClass">
        <span>{{ statusLabel }}</span>
        <small v-if="initialHouse?.reviewMessage">{{ initialHouse.reviewMessage }}</small>
      </div>
    </header>

    <div class="form-grid">
      <label>
        Title
        <input
          v-model.trim="form.title"
          type="text"
          required
          placeholder="Enter listing title"
          :disabled="disabled"
        />
      </label>

      <label>
        Address
        <input
          v-model.trim="form.address"
          type="text"
          required
          placeholder="Enter property address"
          :disabled="disabled"
        />
      </label>

      <label>
        Total price (CNY)
        <input
          v-model.number="form.price"
          type="number"
          min="0"
          step="0.01"
          required
          placeholder="e.g. 2000000"
          :disabled="disabled"
        />
      </label>

      <label>
        Down payment (CNY)
        <input
          v-model.number="form.downPayment"
          type="number"
          min="0"
          step="0.01"
          required
          placeholder="e.g. 600000"
          :disabled="disabled"
        />
      </label>

      <label>
        Floor area (㎡)
        <input
          v-model.number="form.area"
          type="number"
          min="0"
          step="0.01"
          required
          placeholder="e.g. 89"
          :disabled="disabled"
        />
      </label>

      <label>
        Estimated monthly instalment (CNY)
        <input
          :value="formattedInstallment"
          type="text"
          readonly
          placeholder="Calculated automatically from the price, down payment and term"
          :disabled="disabled"
        />
        <small class="hint">The system estimates the monthly instalment based on your down payment and term.</small>
      </label>

      <label>
        Instalment term (months)
        <input
          v-model.number="form.installmentMonths"
          type="number"
          min="1"
          required
          placeholder="e.g. 24"
          :disabled="disabled"
        />
      </label>

      <label>
        Seller username
        <input
          v-model.trim="form.sellerUsername"
          type="text"
          required
          placeholder="Enter seller username"
          :disabled="disabled || disableSellerAccount"
        />
      </label>

      <label>
        Seller display name
        <input
          v-model.trim="form.sellerName"
          type="text"
          required
          placeholder="Enter seller name"
          :disabled="disabled"
        />
      </label>

      <label>
        Contact number
        <input
          v-model.trim="form.contactNumber"
          type="text"
          required
          placeholder="Enter contact number"
          :disabled="disabled"
        />
      </label>

      <label>
        Listing date
        <input v-model="form.listingDate" type="date" required :disabled="disabled" />
      </label>

      <label>
        Floor (optional)
        <input
          v-model.number="form.floor"
          type="number"
          min="0"
          placeholder="e.g. 10"
          :disabled="disabled"
        />
      </label>
    </div>

    <label class="block">
      Listing description
      <textarea
        v-model.trim="form.description"
        rows="3"
        placeholder="Highlight selling points or additional details"
        :disabled="disabled"
      ></textarea>
    </label>

    <label class="block">
      Keywords (separate with commas or spaces)
      <input
        v-model="keywordInput"
        type="text"
        placeholder="e.g. city centre, subway nearby, south-facing"
        :disabled="disabled"
      />
    </label>
    <div v-if="keywordsPreview.length" class="keyword-preview">
      <span class="preview-label">Keywords that will be submitted:</span>
      <ul class="keyword-chips">
        <li v-for="(keyword, index) in keywordsPreview" :key="`${keyword}-${index}`">{{ keyword }}</li>
      </ul>
    </div>

    <section class="images-section">
      <div class="images-header">
        <h4>Listing photos</h4>
        <div class="image-actions">
          <input
            ref="fileInputRef"
            class="file-input"
            type="file"
            accept="image/*"
            multiple
            :disabled="disabled || uploadingImages"
            @change="handleFileSelection"
          />
          <button type="button" class="btn add" :disabled="disabled || uploadingImages" @click="triggerImageUpload">
            {{ uploadingImages ? 'Uploading…' : 'Upload images' }}
          </button>
        </div>
      </div>
      <p class="hint">Upload up to {{ maxImageCount }} images in PNG, JPG, GIF, or WEBP format.</p>
      <p v-if="uploadError" class="error">{{ uploadError }}</p>
      <div v-if="form.imageUrls.length" class="image-gallery">
        <figure v-for="(image, index) in form.imageUrls" :key="`${image}-${index}`" class="image-preview">
          <img :src="image" alt="Listing preview" />
          <figcaption>
            <button type="button" class="btn remove" :disabled="disabled" @click="removeImage(index)">
              Remove
            </button>
          </figcaption>
        </figure>
      </div>
      <p v-else class="hint">No images uploaded yet.</p>
    </section>

    <p v-if="formError" class="error">{{ formError }}</p>

    <div class="actions">
      <button class="btn primary" type="submit" :disabled="disabled">
        {{ loading ? 'Submitting…' : isEditing ? 'Save changes' : 'Submit for review' }}
      </button>
      <button v-if="isEditing" class="btn ghost" type="button" :disabled="disabled" @click="cancelEdit">
        Cancel
      </button>
    </div>
  </form>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';

const props = defineProps({
  initialHouse: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  canManage: {
    type: Boolean,
    default: true
  },
  currentUser: {
    type: Object,
    default: null
  },
  apiBaseUrl: {
    type: String,
    default: 'http://localhost:8080/api'
  }
});

const emit = defineEmits(['submit', 'cancel']);

const listingStatusLabels = {
  PENDING_REVIEW: 'Pending review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  SOLD: 'Sold (unlisted)'
};

const sellerRoles = ['SELLER', 'LANDLORD'];

const form = reactive({
  title: '',
  address: '',
  price: '',
  downPayment: '',
  area: '',
  description: '',
  sellerUsername: '',
  sellerName: '',
  contactNumber: '',
  listingDate: '',
  floor: '',
  installmentMonthlyPayment: '',
  installmentMonths: '',
  imageUrls: []
});

const keywordInput = ref('');
const formError = ref('');
const uploadError = ref('');
const uploadingImages = ref(false);
const fileInputRef = ref(null);
const maxImageCount = 10;

const uploadEndpoint = computed(() => {
  const base = props.apiBaseUrl?.replace(/\/$/, '') ?? '';
  return `${base}/houses/images`;
});

const isSeller = computed(() => sellerRoles.includes(props.currentUser?.role));
const isEditing = computed(() => Boolean(props.initialHouse));
const disabled = computed(() => !props.canManage || props.loading);
const disableSellerAccount = computed(() => isSeller.value);
const initialHouse = computed(() => props.initialHouse);
const statusLabel = computed(() => listingStatusLabels[props.initialHouse?.status] ?? 'Pending review');
const statusClass = computed(() => {
  switch (props.initialHouse?.status) {
    case 'APPROVED':
      return 'status approved';
    case 'REJECTED':
      return 'status rejected';
    case 'SOLD':
      return 'status sold';
    default:
      return 'status pending';
  }
});

const keywordsPreview = computed(() => parseKeywords(keywordInput.value));
const sanitizedImageUrls = computed(() =>
  form.imageUrls
    .map((url) => (typeof url === 'string' ? url.trim() : ''))
    .filter((url) => url.length > 0)
);

const premiumRate = 1.2;

const parsePositiveNumber = (value) => {
  if (value === '' || value == null) {
    return null;
  }
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
};

const calculatedInstallment = computed(() => {
  const price = parsePositiveNumber(form.price);
  const downPayment = parsePositiveNumber(form.downPayment);
  const months = parsePositiveNumber(form.installmentMonths);
  if (!price || !downPayment || !months || months <= 0) {
    return null;
  }
  const totalWithPremium = price * premiumRate;
  const remaining = totalWithPremium - downPayment;
  if (remaining <= 0) {
    return null;
  }
  const monthly = remaining / months;
  return Number.isFinite(monthly) && monthly > 0 ? Number(monthly.toFixed(2)) : null;
});

watch(
  calculatedInstallment,
  (value) => {
    if (value == null) {
      form.installmentMonthlyPayment = '';
    } else {
      form.installmentMonthlyPayment = value;
    }
  },
  { immediate: true }
);

const formattedInstallment = computed(() => {
  if (calculatedInstallment.value == null) {
    return '';
  }
  return calculatedInstallment.value.toFixed(2);
});

const applyImageUrls = (images = []) => {
  const sanitized = Array.isArray(images)
    ? images
        .map((value) => (typeof value === 'string' ? value.trim() : ''))
        .filter((value) => value.length > 0)
    : [];
  form.imageUrls.splice(0, form.imageUrls.length, ...sanitized.slice(0, maxImageCount));
};

const setFormDefaults = () => {
  form.title = '';
  form.address = '';
  form.price = '';
  form.downPayment = '';
  form.area = '';
  form.description = '';
  form.sellerUsername = isSeller.value ? props.currentUser?.username ?? '' : '';
  form.sellerName = isSeller.value ? props.currentUser?.displayName ?? '' : '';
  form.contactNumber = '';
  form.listingDate = '';
  form.floor = '';
  form.installmentMonthlyPayment = '';
  form.installmentMonths = '';
  applyImageUrls();
  keywordInput.value = '';
  formError.value = '';
  uploadError.value = '';
  uploadingImages.value = false;
};

const fillFromHouse = (house) => {
  form.title = house.title ?? '';
  form.address = house.address ?? '';
  form.price = house.price ?? '';
  form.downPayment = house.downPayment ?? '';
  form.area = house.area ?? '';
  form.description = house.description ?? '';
  form.sellerUsername = house.sellerUsername ?? '';
  form.sellerName = house.sellerName ?? '';
  form.contactNumber = house.contactNumber ?? '';
  form.listingDate = house.listingDate ?? '';
  form.floor = house.floor ?? '';
  form.installmentMonthlyPayment = house.installmentMonthlyPayment ?? '';
  form.installmentMonths = house.installmentMonths ?? '';
  applyImageUrls(house.imageUrls ?? []);
  keywordInput.value = Array.isArray(house.keywords) ? house.keywords.join(', ') : '';
  formError.value = '';
  uploadError.value = '';
  uploadingImages.value = false;
};

watch(
  () => props.initialHouse,
  (house) => {
    if (house) {
      fillFromHouse(house);
    } else {
      setFormDefaults();
    }
  },
  { immediate: true }
);

watch(
  () => props.currentUser,
  () => {
    if (!props.initialHouse) {
      setFormDefaults();
    }
  }
);

const parseKeywords = (value) => {
  if (!value) {
    return [];
  }
  return value
    .split(/[，,、\s]+/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
};

const ensurePositive = (value) => {
  if (value === '' || value == null) {
    return false;
  }
  const num = Number(value);
  return Number.isFinite(num) && num > 0;
};

const maxUploadSize = 5 * 1024 * 1024;

const triggerImageUpload = () => {
  if (disabled.value) {
    return;
  }
  if (form.imageUrls.length >= maxImageCount) {
    uploadError.value = `You can upload up to ${maxImageCount} images.`;
    return;
  }
  uploadError.value = '';
  fileInputRef.value?.click();
};

const handleFileSelection = async (event) => {
  if (disabled.value) {
    if (event?.target) {
      event.target.value = '';
    }
    return;
  }
  const input = event?.target;
  const files = Array.from(input?.files ?? []);
  if (input) {
    input.value = '';
  }
  if (!files.length) {
    return;
  }
  const remainingSlots = Math.max(0, maxImageCount - form.imageUrls.length);
  if (remainingSlots <= 0) {
    uploadError.value = `You can upload up to ${maxImageCount} images.`;
    return;
  }
  uploadError.value = '';
  uploadingImages.value = true;
  try {
    for (const file of files.slice(0, remainingSlots)) {
      await uploadImage(file);
    }
    if (files.length > remainingSlots) {
      uploadError.value = `Only ${maxImageCount} images are allowed. Extra files were ignored.`;
    }
  } catch (error) {
    uploadError.value = error instanceof Error ? error.message : 'Failed to upload image. Please try again.';
  } finally {
    uploadingImages.value = false;
  }
};

const uploadImage = async (file) => {
  if (!file || !file.type?.startsWith('image/')) {
    throw new Error('Only image files can be uploaded.');
  }
  if (file.size > maxUploadSize) {
    throw new Error('Each image must be 5 MB or smaller.');
  }
  const formData = new FormData();
  formData.append('file', file);
  const response = await fetch(uploadEndpoint.value, {
    method: 'POST',
    body: formData
  });
  if (!response.ok) {
    throw new Error(await readUploadError(response));
  }
  const payload = await response.json();
  if (!payload || typeof payload.url !== 'string') {
    throw new Error('Image upload failed. Please try again.');
  }
  const normalized = payload.url.trim();
  if (normalized && !form.imageUrls.includes(normalized)) {
    form.imageUrls.push(normalized);
  }
};

const readUploadError = async (response) => {
  try {
    const data = await response.json();
    return data?.detail || data?.message || 'Image upload failed. Please try again.';
  } catch (error) {
    try {
      const text = await response.text();
      return text || 'Image upload failed. Please try again.';
    } catch (innerError) {
      return 'Image upload failed. Please try again.';
    }
  }
};

const removeImage = (index) => {
  if (disabled.value || index < 0 || index >= form.imageUrls.length) {
    return;
  }
  form.imageUrls.splice(index, 1);
};

const validateForm = () => {
  if (!form.title || !form.address || !form.sellerUsername || !form.sellerName || !form.contactNumber) {
    formError.value = 'Please complete all required listing information.';
    return false;
  }
  if (!form.listingDate) {
    formError.value = 'Choose a listing date.';
    return false;
  }
  if (!ensurePositive(form.price)) {
    formError.value = 'Enter a valid total price.';
    return false;
  }
  if (!ensurePositive(form.downPayment)) {
    formError.value = 'Enter a valid down payment amount.';
    return false;
  }
  const priceNumber = Number(form.price);
  const downPaymentNumber = Number(form.downPayment);
  if (Number.isFinite(priceNumber) && Number.isFinite(downPaymentNumber)) {
    const totalWithPremium = priceNumber * premiumRate;
    if (downPaymentNumber >= totalWithPremium) {
      formError.value = 'The down payment must be below 120% of the total price to calculate instalments.';
      return false;
    }
  }
  if (!ensurePositive(form.area)) {
    formError.value = 'Enter a valid floor area.';
    return false;
  }
  if (!ensurePositive(form.installmentMonthlyPayment)) {
    formError.value = 'Unable to calculate a valid monthly instalment. Please review the down payment or term.';
    return false;
  }
  if (!ensurePositive(form.installmentMonths)) {
    formError.value = 'The instalment term must be greater than zero.';
    return false;
  }
  formError.value = '';
  return true;
};

const normalizeNumber = (value) => {
  if (value === '' || value == null) {
    return null;
  }
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
};

const submitForm = () => {
  if (disabled.value) {
    return;
  }
  if (!validateForm()) {
    return;
  }
  const payload = {
    title: form.title.trim(),
    address: form.address.trim(),
    price: normalizeNumber(form.price),
    downPayment: normalizeNumber(form.downPayment),
    area: normalizeNumber(form.area),
    description: form.description ? form.description.trim() : '',
    sellerUsername: form.sellerUsername.trim(),
    sellerName: form.sellerName.trim(),
    contactNumber: form.contactNumber.trim(),
    listingDate: form.listingDate,
    floor: normalizeNumber(form.floor),
    keywords: [...keywordsPreview.value],
    imageUrls: sanitizedImageUrls.value,
    installmentMonthlyPayment: normalizeNumber(form.installmentMonthlyPayment),
    installmentMonths: normalizeNumber(form.installmentMonths)
  };
  emit('submit', payload);
  if (!isEditing.value) {
    setFormDefaults();
  }
};

const cancelEdit = () => {
  emit('cancel');
  setFormDefaults();
};
</script>

<style scoped>
.house-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  padding: 1.85rem;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border);
  backdrop-filter: blur(var(--glass-blur));
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1.1rem;
}

.form-header h2 {
  margin: 0;
  color: var(--color-text-strong);
}

.notice {
  margin: 0.35rem 0 0;
  padding: 0.6rem 0.85rem;
  background: rgba(224, 231, 255, 0.8);
  border-radius: var(--radius-pill);
  color: #4338ca;
  font-size: 0.95rem;
}

.status-indicator {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  padding: 0.5rem 0.95rem;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
  font-weight: 600;
}

.status-indicator.status.approved {
  background: rgba(34, 197, 94, 0.18);
  color: #166534;
}

.status-indicator.status.pending {
  background: rgba(250, 204, 21, 0.18);
  color: #92400e;
}

.status-indicator.status.rejected {
  background: rgba(239, 68, 68, 0.2);
  color: #991b1b;
}

.status-indicator.status.sold {
  background: rgba(148, 163, 184, 0.2);
  color: var(--color-text-strong);
}

:global(body[data-theme='dark']) :deep(.status-indicator.status.approved) {
  background: rgba(74, 222, 128, 0.18);
  color: #bbf7d0;
}

:global(body[data-theme='dark']) :deep(.status-indicator.status.pending) {
  background: rgba(253, 224, 71, 0.18);
  color: #facc15;
}

:global(body[data-theme='dark']) :deep(.status-indicator.status.rejected) {
  background: rgba(248, 113, 113, 0.2);
  color: #fecaca;
}

:global(body[data-theme='dark']) :deep(.status-indicator.status.sold) {
  background: rgba(148, 163, 184, 0.28);
  color: rgba(226, 232, 240, 0.92);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

input,
textarea {
  padding: 0.7rem 0.9rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(148, 163, 184, 0.45);
  background: rgba(248, 250, 252, 0.92);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}

input:focus,
textarea:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.65);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.18);
}

.block textarea {
  min-height: 120px;
  resize: vertical;
}

.keyword-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  align-items: center;
}

.preview-label {
  font-weight: 600;
  color: var(--color-text-soft);
}

.keyword-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.keyword-chips li {
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-size: 0.9rem;
}

.images-section {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding: 1.1rem;
  border: 1px dashed rgba(148, 163, 184, 0.45);
  border-radius: var(--radius-lg);
  background: rgba(248, 250, 252, 0.85);
}

.images-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.images-header h4 {
  margin: 0;
  font-size: 1rem;
  color: var(--color-text-strong);
}

.image-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.file-input {
  display: none;
}

.image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.8rem;
}

.image-preview {
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.35);
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  background: rgba(255, 255, 255, 0.85);
}

.image-preview img {
  width: 100%;
  height: 140px;
  object-fit: cover;
}

.image-preview figcaption {
  padding: 0 0 0.65rem;
  display: flex;
  justify-content: center;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.actions {
  display: flex;
  gap: 0.85rem;
  justify-content: flex-end;
}

.btn {
  padding: 0.7rem 1.35rem;
  border-radius: var(--radius-pill);
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.btn.primary {
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 18px 35px rgba(37, 99, 235, 0.28);
}

.btn.ghost {
  background: transparent;
  color: var(--color-text-strong);
  border: 1px solid rgba(148, 163, 184, 0.35);
}

.btn.add {
  background: var(--gradient-primary);
  color: #fff;
  padding: 0.45rem 0.95rem;
}

.btn.remove {
  background: rgba(254, 226, 226, 0.9);
  color: #b91c1c;
  padding: 0.45rem 0.95rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.error {
  margin: 0;
  color: #dc2626;
  font-size: 0.95rem;
  font-weight: 600;
}

@media (max-width: 768px) {
  .house-form {
    padding: 1.35rem;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .images-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.6rem;
  }

  .actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
