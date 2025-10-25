<template>
  <form class="house-form" @submit.prevent="submitForm">
    <header class="form-header">
      <div>
        <h2>{{ isEditing ? '编辑房源' : '新增房源' }}</h2>
        <p v-if="!canManage" class="notice">
          当前角色仅支持浏览房源，如需发布或维护房源请使用卖家或管理员账号。
        </p>
      </div>
      <div v-if="isEditing" class="status-indicator" :class="statusClass">
        <span>{{ statusLabel }}</span>
        <small v-if="initialHouse?.reviewMessage">{{ initialHouse.reviewMessage }}</small>
      </div>
    </header>

    <div class="form-grid">
      <label>
        标题
        <input
          v-model.trim="form.title"
          type="text"
          required
          placeholder="请输入房源标题"
          :disabled="disabled"
        />
      </label>

      <label>
        地址
        <input
          v-model.trim="form.address"
          type="text"
          required
          placeholder="请输入房源地址"
          :disabled="disabled"
        />
      </label>

      <label>
        价格（元）
        <input
          v-model.number="form.price"
          type="number"
          min="0"
          step="0.01"
          required
          placeholder="例如 2000000"
          :disabled="disabled"
        />
      </label>

      <label>
        首付（元）
        <input
          v-model.number="form.downPayment"
          type="number"
          min="0"
          step="0.01"
          required
          placeholder="例如 600000"
          :disabled="disabled"
        />
      </label>

      <label>
        房源面积（㎡）
        <input
          v-model.number="form.area"
          type="number"
          min="0"
          step="0.01"
          required
          placeholder="例如 89"
          :disabled="disabled"
        />
      </label>

      <label>
        分期月供（元）
        <input
          :value="formattedInstallment"
          type="text"
          readonly
          placeholder="系统根据总价、首付与期数自动计算"
          :disabled="disabled"
        />
        <small class="hint">系统按照“分期金额 × 期数 + 首付 = 总价 × 1.2”自动计算月供。</small>
      </label>

      <label>
        分期期数
        <input
          v-model.number="form.installmentMonths"
          type="number"
          min="1"
          required
          placeholder="例如 24"
          :disabled="disabled"
        />
      </label>

      <label>
        卖家账号
        <input
          v-model.trim="form.sellerUsername"
          type="text"
          required
          placeholder="请输入卖家账号"
          :disabled="disabled || disableSellerAccount"
        />
      </label>

      <label>
        卖家姓名
        <input
          v-model.trim="form.sellerName"
          type="text"
          required
          placeholder="请输入卖家姓名"
          :disabled="disabled"
        />
      </label>

      <label>
        联系方式
        <input
          v-model.trim="form.contactNumber"
          type="text"
          required
          placeholder="请输入联系方式"
          :disabled="disabled"
        />
      </label>

      <label>
        挂牌日期
        <input v-model="form.listingDate" type="date" required :disabled="disabled" />
      </label>

      <label>
        楼层（选填）
        <input
          v-model.number="form.floor"
          type="number"
          min="0"
          placeholder="例如 10"
          :disabled="disabled"
        />
      </label>
    </div>

    <label class="block">
      房源描述
      <textarea
        v-model.trim="form.description"
        rows="3"
        placeholder="补充房源亮点或其他信息"
        :disabled="disabled"
      ></textarea>
    </label>

    <label class="block">
      关键词（使用逗号、空格或顿号分隔）
      <input
        v-model="keywordInput"
        type="text"
        placeholder="例如：学区房，地铁沿线，南北通透"
        :disabled="disabled"
      />
    </label>
    <div v-if="keywordsPreview.length" class="keyword-preview">
      <span class="preview-label">将提交的关键词：</span>
      <ul class="keyword-chips">
        <li v-for="(keyword, index) in keywordsPreview" :key="`${keyword}-${index}`">{{ keyword }}</li>
      </ul>
    </div>

    <section class="images-section">
      <div class="images-header">
        <h4>房源图片</h4>
        <span class="hint">支持上传本地文件，最多 {{ MAX_IMAGES }} 张，首图默认作为封面。</span>
      </div>
      <div class="upload-bar">
        <input
          ref="fileInputRef"
          class="file-input"
          type="file"
          accept="image/*"
          multiple
          :disabled="disabled || imageUploading"
          @change="handleImageSelect"
        />
        <button
          type="button"
          class="btn secondary"
          :disabled="disabled || imageUploading"
          @click="triggerFileDialog"
        >
          选择图片
        </button>
        <span class="upload-hint">
          {{ imageUploading
            ? '图片处理中，请稍候…'
            : remainingSlots > 0
            ? `还能添加 ${remainingSlots} 张图片`
            : '已达到上传上限' }}
        </span>
      </div>
      <p v-if="imageError" class="error">{{ imageError }}</p>
      <ul v-if="form.images.length" class="image-preview">
        <li v-for="(image, index) in form.images" :key="image.id" class="image-card">
          <img :src="image.url" :alt="image.name" />
          <footer>
            <span class="image-name">{{ image.name }}</span>
            <button
              type="button"
              class="btn remove"
              :disabled="disabled || imageUploading"
              @click="removeImage(index)"
            >
              删除
            </button>
          </footer>
        </li>
      </ul>
    </section>

    <p v-if="formError" class="error">{{ formError }}</p>

    <div class="actions">
      <button class="btn primary" type="submit" :disabled="disabled">
        {{ loading ? '提交中...' : isEditing ? '保存修改' : '提交审核' }}
      </button>
      <button v-if="isEditing" class="btn ghost" type="button" :disabled="disabled" @click="cancelEdit">
        取消编辑
      </button>
    </div>
  </form>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';

const MAX_IMAGES = 8;

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
  }
});

const emit = defineEmits(['submit', 'cancel']);

const listingStatusLabels = {
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
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
  images: []
});

const keywordInput = ref('');
const formError = ref('');
const fileInputRef = ref(null);
const imageError = ref('');
const imageUploading = ref(false);

const isSeller = computed(() => sellerRoles.includes(props.currentUser?.role));
const isEditing = computed(() => Boolean(props.initialHouse));
const disabled = computed(() => !props.canManage || props.loading);
const disableSellerAccount = computed(() => isSeller.value);
const initialHouse = computed(() => props.initialHouse);
const statusLabel = computed(() => listingStatusLabels[props.initialHouse?.status] ?? '待审核');
const statusClass = computed(() => {
  switch (props.initialHouse?.status) {
    case 'APPROVED':
      return 'status approved';
    case 'REJECTED':
      return 'status rejected';
    default:
      return 'status pending';
  }
});

const keywordsPreview = computed(() => parseKeywords(keywordInput.value));
const imagePayload = computed(() =>
  form.images
    .map((image) => (typeof image.url === 'string' ? image.url.trim() : ''))
    .filter((url) => url.length > 0)
);

const remainingSlots = computed(() => Math.max(0, MAX_IMAGES - form.images.length));

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

const buildImageEntry = (url, name = '已上传图片') => ({
  id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  url,
  name
});

const applyImageUrls = (images = []) => {
  const entries = Array.isArray(images)
    ? images
        .filter((item) => typeof item === 'string' && item.trim().length > 0)
        .map((item, index) => buildImageEntry(item.trim(), `已上传图片 ${index + 1}`))
    : [];
  form.images.splice(0, form.images.length, ...entries);
  imageError.value = '';
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
  keywordInput.value = Array.isArray(house.keywords) ? house.keywords.join('、') : '';
  formError.value = '';
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

const resetFileInput = () => {
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

const triggerFileDialog = () => {
  if (disabled.value || imageUploading.value) {
    return;
  }
  fileInputRef.value?.click();
};

const readFileAsDataUrl = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = (event) => reject(event);
    reader.readAsDataURL(file);
  });

const handleImageSelect = async (event) => {
  if (disabled.value) {
    resetFileInput();
    return;
  }
  const files = Array.from(event.target?.files ?? []);
  if (!files.length) {
    resetFileInput();
    return;
  }
  if (remainingSlots.value <= 0) {
    imageError.value = `最多只能上传 ${MAX_IMAGES} 张图片。`;
    resetFileInput();
    return;
  }

  const allowedFiles = files.slice(0, remainingSlots.value);
  imageUploading.value = true;
  imageError.value = '';

  try {
    const results = await Promise.all(
      allowedFiles.map(async (file) => {
        const url = await readFileAsDataUrl(file);
        return buildImageEntry(url, file.name);
      })
    );
    form.images.push(...results);
  } catch (error) {
    console.error('读取图片失败：', error);
    imageError.value = '读取图片失败，请重试。';
  } finally {
    imageUploading.value = false;
    resetFileInput();
  }
};

const removeImage = (index) => {
  if (disabled.value || imageUploading.value) {
    return;
  }
  form.images.splice(index, 1);
};

const ensurePositive = (value) => {
  if (value === '' || value == null) {
    return false;
  }
  const num = Number(value);
  return Number.isFinite(num) && num > 0;
};

const validateForm = () => {
  if (!form.title || !form.address || !form.sellerUsername || !form.sellerName || !form.contactNumber) {
    formError.value = '请完整填写房源基础信息。';
    return false;
  }
  if (!form.listingDate) {
    formError.value = '请选择挂牌日期。';
    return false;
  }
  if (!ensurePositive(form.price)) {
    formError.value = '请填写有效的房源价格。';
    return false;
  }
  if (!ensurePositive(form.downPayment)) {
    formError.value = '请填写有效的首付金额。';
    return false;
  }
  const priceNumber = Number(form.price);
  const downPaymentNumber = Number(form.downPayment);
  if (Number.isFinite(priceNumber) && Number.isFinite(downPaymentNumber)) {
    const totalWithPremium = priceNumber * premiumRate;
    if (downPaymentNumber >= totalWithPremium) {
      formError.value = '首付金额必须低于总价的120%，以便计算分期。';
      return false;
    }
  }
  if (!ensurePositive(form.area)) {
    formError.value = '请填写有效的房源面积。';
    return false;
  }
  if (!ensurePositive(form.installmentMonthlyPayment)) {
    formError.value = '无法根据公式计算有效的分期月供，请检查首付或分期期数。';
    return false;
  }
  if (!ensurePositive(form.installmentMonths)) {
    formError.value = '分期期数必须大于0。';
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
    imageUrls: imagePayload.value,
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
  gap: 0.9rem;
  padding: 1.1rem;
  border: 1px dashed rgba(148, 163, 184, 0.45);
  border-radius: var(--radius-lg);
  background: rgba(248, 250, 252, 0.85);
}

.images-header {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.images-header h4 {
  margin: 0;
  font-size: 1.05rem;
  color: var(--color-text-strong);
}

.images-header .hint {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.upload-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.file-input {
  display: none;
}

.upload-bar .btn.secondary {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.45);
  color: var(--color-text-strong);
  padding: 0.5rem 1.25rem;
}

.upload-bar .btn.secondary:not(:disabled):hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.98);
}

.upload-hint {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.image-preview {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 0.85rem;
}

.image-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.image-card img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
}

.image-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.5rem 0.65rem 0.65rem;
}

.image-card .image-name {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.btn.secondary {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.35);
  color: var(--color-text-strong);
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
