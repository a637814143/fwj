<template>
  <form class="house-form" @submit.prevent="submitForm">
    <header class="form-header">
      <div>
        <h2>{{ isEditing ? t('manage.form.headingEdit') : t('manage.form.headingCreate') }}</h2>
        <p v-if="!canManage" class="notice">
          {{ t('manage.form.noticeBrowseOnly') }}
        </p>
      </div>
      <div v-if="isEditing" class="status-indicator" :class="statusClass">
        <span>{{ statusLabel }}</span>
        <small v-if="initialHouse?.reviewMessage">{{ initialHouse.reviewMessage }}</small>
      </div>
    </header>

    <div class="form-grid">
      <label>
        {{ t('manage.form.fields.title') }}
        <input
          v-model.trim="form.title"
          type="text"
          required
          :placeholder="t('manage.form.placeholders.title')"
          :disabled="disabled"
        />
      </label>

      <label>
        {{ t('manage.form.fields.address') }}
        <input
          v-model.trim="form.addressDetail"
          type="text"
          required
          :placeholder="t('manage.form.placeholders.address')"
          :disabled="disabled"
        />
      </label>

      <div class="location-group" aria-label="地段选择">
        <label>
          省份
          <select v-model="form.location.province" :disabled="disabled">
            <option value="云南省">云南省</option>
          </select>
        </label>
        <label>
          地州
          <select v-model="form.location.city" :disabled="disabled">
            <option value="" disabled>请选择地州</option>
            <option v-for="city in Object.keys(yunnanRegions)" :key="city" :value="city">{{ city }}</option>
          </select>
        </label>
        <label>
          县区
          <select v-model="form.location.county" :disabled="disabled || !form.location.city">
            <option value="" disabled>请选择县区</option>
            <option
              v-for="county in (yunnanRegions[form.location.city] || [])"
              :key="county"
              :value="county"
            >
              {{ county }}
            </option>
          </select>
        </label>
      </div>

      <label>
        {{ t('manage.form.fields.price') }}
        <input
          v-model.number="form.price"
          type="number"
          min="0"
          step="0.01"
          required
          :placeholder="t('manage.form.placeholders.price')"
          :disabled="disabled"
        />
      </label>

      <label>
        {{ t('manage.form.fields.downPayment') }}
        <input
          v-model.number="form.downPayment"
          type="number"
          min="0"
          step="0.01"
          required
          :placeholder="t('manage.form.placeholders.downPayment')"
          :disabled="disabled"
        />
      </label>

      <label>
        {{ t('manage.form.fields.area') }}
        <input
          v-model.number="form.area"
          type="number"
          min="0"
          step="0.01"
          required
          :placeholder="t('manage.form.placeholders.area')"
          :disabled="disabled"
        />
      </label>

      <label>
        {{ t('manage.form.fields.sellerUsername') }}
        <input
          v-model.trim="form.sellerUsername"
          type="text"
          required
          :placeholder="t('manage.form.placeholders.sellerUsername')"
          :disabled="disabled || disableSellerAccount"
        />
      </label>

      <label>
        {{ t('manage.form.fields.sellerName') }}
        <input
          v-model.trim="form.sellerName"
          type="text"
          required
          :placeholder="t('manage.form.placeholders.sellerName')"
          :disabled="disabled"
        />
      </label>

      <label>
        {{ t('manage.form.fields.contactNumber') }}
        <input
          v-model.trim="form.contactNumber"
          type="text"
          required
          :placeholder="t('manage.form.placeholders.contactNumber')"
          :disabled="disabled"
        />
      </label>

      <label>
        {{ t('manage.form.fields.listingDate') }}
        <input
          v-model="form.listingDate"
          type="date"
          required
          :min="minListingDate"
          :disabled="disabled"
        />
      </label>

      <label>
        {{ t('manage.form.fields.floor') }}
        <input
          v-model.number="form.floor"
          type="number"
          min="0"
          :placeholder="t('manage.form.placeholders.floor')"
          :disabled="disabled"
        />
      </label>

    </div>

    <label class="block">
      {{ t('manage.form.fields.description') }}
      <textarea
        v-model.trim="form.description"
        rows="3"
        :placeholder="t('manage.form.placeholders.description')"
        :disabled="disabled"
      ></textarea>
    </label>

    <label class="block">
      {{ t('manage.form.fields.keywords') }}
      <input
        v-model="keywordInput"
        type="text"
        :placeholder="t('manage.form.placeholders.keywords')"
        :disabled="disabled"
      />
    </label>
    <div v-if="keywordsPreview.length" class="keyword-preview">
      <span class="preview-label">{{ t('manage.form.hints.keywordPreview') }}</span>
      <ul class="keyword-chips">
        <li v-for="(keyword, index) in keywordsPreview" :key="`${keyword}-${index}`">{{ keyword }}</li>
      </ul>
    </div>

    <section class="images-section">
      <div class="images-header">
        <h4>{{ t('manage.form.fields.images') }}</h4>
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
            {{ uploadingImages ? t('manage.form.actions.uploading') : t('manage.form.actions.upload') }}
          </button>
        </div>
      </div>
      <p class="hint">{{ t('manage.form.hints.uploadLimit', { count: maxImageCount }) }}</p>
      <p v-if="uploadError" class="error">{{ uploadError }}</p>
      <div v-if="form.imageUrls.length" class="image-gallery">
        <figure v-for="(image, index) in form.imageUrls" :key="`${image}-${index}`" class="image-preview">
          <img :src="image" :alt="t('manage.form.imageAlt', { index: index + 1 })" />
          <figcaption>
            <button type="button" class="btn remove" :disabled="disabled" @click="removeImage(index)">
              {{ t('manage.form.actions.removeImage') }}
            </button>
          </figcaption>
        </figure>
      </div>
      <p v-else class="hint">{{ t('manage.form.hints.noImages') }}</p>
    </section>

    <section class="contract-section">
      <div>
        <h4>{{ t('contracts.usage.seller.title') }}</h4>
        <p class="hint">{{ t('contracts.usage.seller.notice') }}</p>
      </div>
      <div class="contract-actions">
        <button type="button" class="btn link" :disabled="disabled" @click="openSellerContract">
          {{ t('contracts.common.open') }}
        </button>
        <span :class="['contract-status', { accepted: sellerContractAccepted }]">
          {{ sellerContractAccepted ? t('contracts.status.accepted') : t('contracts.status.pending') }}
        </span>
      </div>
    </section>

    <p v-if="formError" class="error">{{ formError }}</p>

    <div class="actions">
      <button
        class="btn secondary"
        type="button"
        :disabled="disabled"
        @click="submitForm({ draft: true })"
      >
        {{ t('manage.form.actions.saveDraft') }}
      </button>
      <button class="btn primary" type="submit" :disabled="disabled">
        {{
          loading
            ? t('manage.form.actions.submitting')
            : isEditing
            ? t('manage.form.actions.submitEdit')
            : t('manage.form.actions.submitCreate')
        }}
      </button>
      <button v-if="isEditing" class="btn ghost" type="button" :disabled="disabled" @click="cancelEdit">
        {{ t('manage.form.actions.cancel') }}
      </button>
    </div>

    <ContractAgreement
      :visible="sellerContractVisible"
      context="seller"
      @agree="handleSellerContractAgree"
      @reject="handleSellerContractReject"
    />
  </form>
</template>

<script setup>
import { computed, inject, reactive, ref, watch } from 'vue';
import ContractAgreement from './ContractAgreement.vue';

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
  resetKey: {
    type: Number,
    default: 0
  },
  apiBaseUrl: {
    type: String,
    default: 'http://localhost:8080/api'
  }
});

const emit = defineEmits(['submit', 'cancel']);

const translate = inject('translate', (key, vars) => {
  if (typeof key !== 'string') {
    return '';
  }
  if (!vars) {
    return key;
  }
  return key;
});

const t = (key, vars) => translate(key, vars);

const listingStatusLabels = computed(() => ({
  DRAFT: t('statuses.draft'),
  PENDING_REVIEW: t('statuses.pending'),
  APPROVED: t('statuses.approved'),
  REJECTED: t('statuses.rejected'),
  SOLD: t('statuses.sold')
}));

const keywordSeparator = computed(() => {
  const separator = t('manage.list.keywordSeparator');
  return typeof separator === 'string' && separator.length ? separator : ', ';
});

const sellerRoles = ['SELLER', 'LANDLORD'];

const yunnanRegions = {
  昆明市: ['五华区', '盘龙区', '官渡区', '西山区', '呈贡区', '东川区', '晋宁区', '安宁市', '富民县', '嵩明县', '宜良县', '石林彝族自治县', '禄劝彝族苗族自治县', '寻甸回族彝族自治县'],
  曲靖市: ['麒麟区', '沾益区', '马龙区', '宣威市', '陆良县', '师宗县', '罗平县', '富源县', '会泽县'],
  玉溪市: ['红塔区', '江川区', '通海县', '华宁县', '易门县', '峨山彝族自治县', '新平彝族傣族自治县', '元江哈尼族彝族傣族自治县'],
  保山市: ['隆阳区', '施甸县', '龙陵县', '昌宁县', '腾冲市'],
  昭通市: ['昭阳区', '水富市', '鲁甸县', '巧家县', '盐津县', '大关县', '永善县', '绥江县', '镇雄县', '彝良县', '威信县'],
  丽江市: ['古城区', '永胜县', '华坪县', '玉龙纳西族自治县', '宁蒗彝族自治县'],
  普洱市: ['思茅区', '宁洱哈尼族彝族自治县', '墨江哈尼族自治县', '景东彝族自治县', '景谷傣族彝族自治县', '镇沅彝族哈尼族拉祜族自治县', '江城哈尼族彝族自治县', '孟连傣族拉祜族佤族自治县', '澜沧拉祜族自治县', '西盟佤族自治县'],
  临沧市: ['临翔区', '凤庆县', '云县', '永德县', '镇康县', '双江拉祜族佤族布朗族傣族自治县', '耿马傣族佤族自治县', '沧源佤族自治县'],
  文山壮族苗族自治州: ['文山市', '砚山县', '西畴县', '麻栗坡县', '马关县', '丘北县', '广南县', '富宁县'],
  红河哈尼族彝族自治州: ['蒙自市', '个旧市', '开远市', '弥勒市', '绿春县', '建水县', '石屏县', '泸西县', '元阳县', '红河县', '金平苗族瑶族傣族自治县', '河口瑶族自治县', '屏边苗族自治县'],
  西双版纳傣族自治州: ['景洪市', '勐海县', '勐腊县'],
  楚雄彝族自治州: ['楚雄市', '双柏县', '牟定县', '南华县', '姚安县', '大姚县', '永仁县', '元谋县', '武定县', '禄丰县'],
  大理白族自治州: ['大理市', '祥云县', '宾川县', '弥渡县', '永平县', '云龙县', '洱源县', '剑川县', '鹤庆县', '漾濞彝族自治县', '南涧彝族自治县', '巍山彝族回族自治县'],
  德宏傣族景颇族自治州: ['芒市', '瑞丽市', '梁河县', '盈江县', '陇川县'],
  怒江傈僳族自治州: ['泸水市', '福贡县', '贡山独龙族怒族自治县', '兰坪白族普米族自治县'],
  迪庆藏族自治州: ['香格里拉市', '德钦县', '维西傈僳族自治县']
};

const imageExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];
const formatDate = (date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
    return '';
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const minListingDate = computed(() => {
  const tomorrow = new Date();
  tomorrow.setHours(0, 0, 0, 0);
  tomorrow.setDate(tomorrow.getDate() + 1);
  return formatDate(tomorrow);
});

const assetOrigin = computed(() => {
  const base = props.apiBaseUrl ?? '';
  try {
    return new URL(base).origin;
  } catch (error) {
    if (typeof window !== 'undefined') {
      try {
        return new URL(base, window.location.origin).origin;
      } catch (innerError) {
        return window.location.origin;
      }
    }
    return '';
  }
});

const resolveAssetUrl = (raw) => {
  if (raw == null) {
    return '';
  }
  const trimmed = String(raw).trim();
  if (!trimmed) {
    return '';
  }
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }
  if (/^\/\//.test(trimmed)) {
    if (typeof window !== 'undefined') {
      return `${window.location.protocol}${trimmed}`;
    }
    return `http:${trimmed}`;
  }
  const origin = assetOrigin.value;
  if (!origin) {
    return trimmed;
  }
  try {
    return new URL(trimmed, origin).toString();
  } catch (error) {
    try {
      return new URL(trimmed, props.apiBaseUrl).toString();
    } catch (innerError) {
      return trimmed;
    }
  }
};

const form = reactive({
  title: '',
  addressDetail: '',
  location: {
    province: '云南省',
    city: '',
    county: ''
  },
  price: '',
  downPayment: '',
  area: '',
  description: '',
  sellerUsername: '',
  sellerName: '',
  contactNumber: '',
  listingDate: '',
  floor: '',
  imageUrls: []
});

const keywordInput = ref('');
const formError = ref('');
const uploadError = ref('');
const uploadingImages = ref(false);
const fileInputRef = ref(null);
const maxImageCount = 10;
const sellerContractVisible = ref(false);
const sellerContractAccepted = ref(false);

const uploadEndpoint = computed(() => {
  const base = props.apiBaseUrl?.replace(/\/$/, '') ?? '';
  return `${base}/houses/images`;
});

const isSeller = computed(() => sellerRoles.includes(props.currentUser?.role));
const isEditing = computed(() => Boolean(props.initialHouse));
const disabled = computed(() => !props.canManage || props.loading);
const disableSellerAccount = computed(() => isSeller.value);
const initialHouse = computed(() => props.initialHouse);
const statusLabel = computed(
  () => listingStatusLabels.value[props.initialHouse?.status] ?? t('statuses.pending')
);
const statusClass = computed(() => {
  switch (props.initialHouse?.status) {
    case 'DRAFT':
      return 'status draft';
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
    .map((url) => resolveAssetUrl(typeof url === 'string' ? url : ''))
    .filter((url) => url.length > 0)
);

const formHasContent = computed(() => {
  return Boolean(
    form.title ||
      form.addressDetail ||
      form.location.city ||
      form.location.county ||
      form.price ||
      form.downPayment ||
      form.area ||
      form.description ||
      form.sellerUsername ||
      form.sellerName ||
      form.contactNumber ||
      form.listingDate ||
      form.floor ||
      keywordInput.value ||
      form.imageUrls.length
  );
});

const premiumRate = 1.2;

const isImageUrl = (url) => {
  if (!url) {
    return false;
  }
  const normalized = String(url).trim().toLowerCase();
  return imageExtensions.some((ext) => normalized.endsWith(ext));
};

const parseLocalDate = (value) => {
  if (!value) {
    return null;
  }
  const normalized = `${value}T00:00:00`;
  const parsed = new Date(normalized);
  if (Number.isNaN(parsed.getTime())) {
    return null;
  }
  parsed.setHours(0, 0, 0, 0);
  return parsed;
};

const isListingDateFuture = (value) => {
  const target = parseLocalDate(value);
  if (!target) {
    return false;
  }
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return target.getTime() > today.getTime();
};

const applyImageUrls = (images = []) => {
  const sanitized = Array.isArray(images)
    ? images
        .map((value) => (typeof value === 'string' ? resolveAssetUrl(value) : ''))
        .filter((value) => value.length > 0)
    : [];
  form.imageUrls.splice(0, form.imageUrls.length, ...sanitized.slice(0, maxImageCount));
};

const setFormDefaults = () => {
  form.title = '';
  form.addressDetail = '';
  form.location.province = '云南省';
  form.location.city = '';
  form.location.county = '';
  form.price = '';
  form.downPayment = '';
  form.area = '';
  form.description = '';
  form.sellerUsername = isSeller.value ? props.currentUser?.username ?? '' : '';
  form.sellerName = isSeller.value ? props.currentUser?.displayName ?? '' : '';
  form.contactNumber = '';
  form.listingDate = '';
  form.floor = '';
  applyImageUrls();
  keywordInput.value = '';
  formError.value = '';
  uploadError.value = '';
  uploadingImages.value = false;
  sellerContractVisible.value = false;
  sellerContractAccepted.value = Boolean(props.initialHouse);
};

const fillFromHouse = (house) => {
  form.title = house.title ?? '';
  const addressText = house.address ?? '';
  if (typeof addressText === 'string' && addressText.includes('云南省')) {
    const parts = addressText.split(/-/);
    form.location.province = parts[0] || '云南省';
    form.location.city = parts[1] || '';
    form.location.county = parts[2] || '';
    form.addressDetail = parts.slice(3).join('-') || '';
  } else {
    form.addressDetail = addressText;
    form.location.province = '云南省';
    form.location.city = '';
    form.location.county = '';
  }
  form.price = house.price ?? '';
  form.downPayment = house.downPayment ?? '';
  form.area = house.area ?? '';
  form.description = house.description ?? '';
  form.sellerUsername = house.sellerUsername ?? '';
  form.sellerName = house.sellerName ?? '';
  form.contactNumber = house.contactNumber ?? '';
  form.listingDate = house.listingDate ?? '';
  form.floor = house.floor ?? '';
  applyImageUrls(house.imageUrls ?? []);
  keywordInput.value = Array.isArray(house.keywords)
    ? house.keywords.join(keywordSeparator.value)
    : '';
  formError.value = '';
  uploadError.value = '';
  uploadingImages.value = false;
  sellerContractVisible.value = false;
  sellerContractAccepted.value = true;
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
    if (props.initialHouse) {
      return;
    }
    if (!formHasContent.value) {
      setFormDefaults();
    } else if (isSeller.value) {
      if (!form.sellerUsername) {
        form.sellerUsername = props.currentUser?.username ?? '';
      }
      if (!form.sellerName) {
        form.sellerName = props.currentUser?.displayName ?? '';
      }
    }
  }
);

watch(
  () => props.resetKey,
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
    uploadError.value = t('manage.form.upload.limit', { count: maxImageCount });
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
    uploadError.value = t('manage.form.upload.limit', { count: maxImageCount });
    return;
  }
  uploadError.value = '';
  uploadingImages.value = true;
  try {
    for (const file of files.slice(0, remainingSlots)) {
      await uploadImage(file);
    }
    if (files.length > remainingSlots) {
      uploadError.value = t('manage.form.upload.extraIgnored', { count: maxImageCount });
    }
  } catch (error) {
    uploadError.value =
      error instanceof Error ? error.message : t('manage.form.upload.failure');
  } finally {
    uploadingImages.value = false;
  }
};

const uploadImage = async (file) => {
  if (!file || !file.type?.startsWith('image/')) {
    throw new Error(t('manage.form.upload.invalidType'));
  }
  if (file.size > maxUploadSize) {
    throw new Error(t('manage.form.upload.tooLarge'));
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
    throw new Error(t('manage.form.upload.failure'));
  }
  const normalized = resolveAssetUrl(payload.url);
  if (normalized && !form.imageUrls.includes(normalized)) {
    form.imageUrls.push(normalized);
  }
};

const readUploadError = async (response) => {
  try {
    const data = await response.json();
    return data?.detail || data?.message || t('manage.form.upload.failure');
  } catch (error) {
    try {
      const text = await response.text();
      return text || t('manage.form.upload.failure');
    } catch (innerError) {
      return t('manage.form.upload.failure');
    }
  }
};

const removeImage = (index) => {
  if (disabled.value || index < 0 || index >= form.imageUrls.length) {
    return;
  }
  form.imageUrls.splice(index, 1);
};

const validateForm = ({ draft = false } = {}) => {
  if (
    !form.title ||
    !form.addressDetail ||
    !form.location.city ||
    !form.location.county ||
    !form.sellerUsername ||
    !form.sellerName ||
    !form.contactNumber
  ) {
    formError.value = t('manage.form.validation.required');
    return false;
  }
  if (!form.listingDate) {
    formError.value = t('manage.form.validation.listingDate');
    return false;
  }
  if (!isListingDateFuture(form.listingDate)) {
    formError.value = t('manage.form.validation.listingDateFuture');
    return false;
  }
  if (!ensurePositive(form.price)) {
    formError.value = t('manage.form.validation.price');
    return false;
  }
  if (!ensurePositive(form.downPayment)) {
    formError.value = t('manage.form.validation.downPayment');
    return false;
  }
  const priceNumber = Number(form.price);
  const downPaymentNumber = Number(form.downPayment);
  if (Number.isFinite(priceNumber) && Number.isFinite(downPaymentNumber)) {
    const totalWithPremium = priceNumber * premiumRate;
    if (downPaymentNumber >= totalWithPremium) {
      formError.value = t('manage.form.validation.downPaymentLimit');
      return false;
    }
  }
  if (!ensurePositive(form.area)) {
    formError.value = t('manage.form.validation.area');
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

const submitForm = (maybeOptions) => {
  const options =
    maybeOptions && typeof maybeOptions === 'object' && 'draft' in maybeOptions ? maybeOptions : { draft: false };
  const draft = Boolean(options.draft);
  if (disabled.value) {
    return;
  }
  if (!validateForm({ draft })) {
    return;
  }
  if (!draft && !sellerContractAccepted.value) {
    formError.value = t('contracts.errors.sellerAgreeRequired');
    sellerContractVisible.value = true;
    return;
  }
  const address = [form.location.province, form.location.city, form.location.county, form.addressDetail]
    .filter(Boolean)
    .join('-');
  const payload = {
    title: form.title.trim(),
    address,
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
    imageUrls: sanitizedImageUrls.value
  };
  emit('submit', { payload, draft });
};

const cancelEdit = () => {
  emit('cancel');
  setFormDefaults();
};

const openSellerContract = () => {
  if (disabled.value) {
    return;
  }
  formError.value = '';
  sellerContractVisible.value = true;
};

const handleSellerContractAgree = () => {
  sellerContractAccepted.value = true;
  sellerContractVisible.value = false;
  formError.value = '';
};

const handleSellerContractReject = () => {
  sellerContractAccepted.value = false;
  sellerContractVisible.value = false;
  formError.value = t('contracts.errors.sellerDeclined');
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

.status-indicator.status.draft {
  background: rgba(191, 219, 254, 0.3);
  color: #1d4ed8;
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

:global(body[data-theme='dark']) :deep(.status-indicator.status.draft) {
  background: rgba(96, 165, 250, 0.22);
  color: #93c5fd;
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

.location-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--panel-card-bg) 90%, transparent);
  border: 1px dashed rgba(59, 130, 246, 0.35);
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

.contract-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: rgba(59, 130, 246, 0.06);
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-primary, #2563eb) 20%, transparent);
}

.contract-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.btn.link {
  background: none;
  border: none;
  color: var(--color-primary, #2563eb);
  padding: 0;
  font-weight: 600;
  cursor: pointer;
}

.btn.link:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.contract-status {
  font-size: 0.95rem;
  color: var(--color-text-muted);
}

.contract-status.accepted {
  color: #047857;
  font-weight: 600;
}

:global(body[data-theme='dark']) .contract-section {
  background: rgba(37, 99, 235, 0.12);
  border-color: rgba(96, 165, 250, 0.35);
}

:global(body[data-theme='dark']) .btn.link {
  color: #93c5fd;
}

:global(body[data-theme='dark']) .contract-status {
  color: rgba(148, 163, 184, 0.85);
}

:global(body[data-theme='dark']) .contract-status.accepted {
  color: #34d399;
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

.btn.secondary {
  background: rgba(219, 234, 254, 0.85);
  color: #1d4ed8;
  border: 1px solid rgba(59, 130, 246, 0.35);
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

:global(body[data-theme='dark']) :deep(.btn.secondary) {
  background: rgba(37, 99, 235, 0.18);
  color: #bfdbfe;
  border-color: rgba(96, 165, 250, 0.6);
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
