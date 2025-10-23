<template>
  <form class="house-form" @submit.prevent="submitForm">
    <header class="form-header">
      <div>
        <h2>{{ isEditing ? '编辑房源' : '新增房源' }}</h2>
        <p v-if="!canManage" class="notice">
          当前角色仅支持浏览房源，如需发布或维护房源请使用卖家账号。
        </p>
      </div>
      <div v-if="isEditing" class="status-indicator" :class="statusClass">
        <span>{{ statusLabel }}</span>
        <small v-if="initialHouse?.reviewMessage">{{ initialHouse.reviewMessage }}</small>
      </div>
    </header>

    <nav class="stepper" aria-label="发布步骤">
      <button
        v-for="step in steps"
        :key="step.value"
        type="button"
        :class="['step', { active: step.value === currentStep, done: step.value < currentStep }]"
        @click="goToStep(step.value)"
      >
        <span class="index">{{ step.value }}</span>
        <span>{{ step.label }}</span>
      </button>
    </nav>

    <section v-if="currentStep === 1" class="step-panel">
      <h3>填写房源基础信息</h3>
      <div class="form-grid">
        <label>
          标题
          <input v-model.trim="form.title" type="text" required :disabled="disabled" placeholder="请输入房源标题" />
        </label>
        <label>
          地址
          <input v-model.trim="form.address" type="text" required :disabled="disabled" placeholder="请输入房源地址" />
        </label>
        <label>
          卖家账号
          <input
            v-model.trim="form.sellerUsername"
            type="text"
            required
            :disabled="disabled || disableSellerAccount"
            placeholder="请输入卖家账号"
          />
        </label>
        <label>
          卖家姓名
          <input v-model.trim="form.sellerName" type="text" required :disabled="disabled" placeholder="请输入卖家姓名" />
        </label>
        <label>
          联系方式
          <input v-model.trim="form.contactNumber" type="text" required :disabled="disabled" placeholder="请输入联系方式" />
        </label>
        <label>
          挂牌日期
          <input v-model="form.listingDate" type="date" required :disabled="disabled" />
        </label>
        <label>
          楼层（选填）
          <input v-model.number="form.floor" type="number" min="0" :disabled="disabled" placeholder="例如 10" />
        </label>
        <label>
          房源面积（㎡）
          <input v-model.number="form.area" type="number" min="0" step="0.01" required :disabled="disabled" />
        </label>
      </div>
      <label class="block">
        房源描述
        <textarea
          v-model.trim="form.description"
          rows="3"
          :disabled="disabled"
          placeholder="补充房源亮点或其他信息"
        ></textarea>
      </label>
      <label class="block">
        关键词（以逗号、空格或顿号分隔）
        <input
          v-model.trim="form.keywordInput"
          type="text"
          :disabled="disabled"
          placeholder="例如：学区房，南北通透，地铁沿线"
        />
      </label>
      <p v-if="stepErrors[1]" class="error">{{ stepErrors[1] }}</p>
      <div class="navigation">
        <span></span>
        <button type="button" class="btn primary" :disabled="disabled" @click="nextStep">下一步</button>
      </div>
    </section>

    <section v-else-if="currentStep === 2" class="step-panel">
      <h3>上传图片并设置定价</h3>
      <div class="pricing-grid">
        <label>
          全款价格（万元）
          <input v-model.number="form.price" type="number" min="0" step="0.01" required :disabled="disabled" />
        </label>
        <label>
          分期月供（万元）
          <input
            v-model.number="form.installmentMonthlyPayment"
            type="number"
            min="0"
            step="0.01"
            required
            :disabled="disabled"
          />
        </label>
        <label>
          分期期数
          <input v-model.number="form.installmentMonths" type="number" min="1" required :disabled="disabled" />
        </label>
      </div>
      <section class="images-section">
        <div class="images-header">
          <h4>房源图片链接</h4>
          <button type="button" class="btn add" :disabled="disabled" @click="addImageField">添加图片</button>
        </div>
        <p class="hint">支持填写多张图片的网络链接，提交前会自动忽略空白链接。</p>
        <div class="image-inputs">
          <div v-for="(image, index) in form.imageUrls" :key="index" class="image-input">
            <input
              v-model.trim="form.imageUrls[index]"
              type="url"
              :disabled="disabled"
              placeholder="例如：https://example.com/house.jpg"
            />
            <button
              type="button"
              class="btn remove"
              :disabled="disabled || form.imageUrls.length === 1"
              @click="removeImageField(index)"
            >
              删除
            </button>
          </div>
        </div>
      </section>
      <p v-if="stepErrors[2]" class="error">{{ stepErrors[2] }}</p>
      <div class="navigation">
        <button type="button" class="btn secondary" @click="previousStep">上一步</button>
        <button type="button" class="btn primary" :disabled="disabled" @click="nextStep">下一步</button>
      </div>
    </section>

    <section v-else class="step-panel">
      <h3>上传产权证明并提交审核</h3>
      <label class="block">
        产权证明链接
        <input
          v-model.trim="form.propertyCertificateUrl"
          type="url"
          required
          :disabled="disabled"
          placeholder="请输入网盘或文件访问链接"
        />
      </label>
      <article class="summary">
        <h4>提交前确认</h4>
        <ul>
          <li>房源标题：{{ form.title || '—' }}</li>
          <li>全款价格：￥{{ formatNumber(form.price) }} 万</li>
          <li>分期方案：￥{{ formatNumber(form.installmentMonthlyPayment) }} 万 × {{ form.installmentMonths || '—' }} 期</li>
          <li>关键词：{{ keywordsPreview }}</li>
        </ul>
      </article>
      <p v-if="stepErrors[3]" class="error">{{ stepErrors[3] }}</p>
      <div class="navigation">
        <button type="button" class="btn secondary" @click="previousStep">上一步</button>
        <div class="actions">
          <button class="btn primary" type="submit" :disabled="disabled">
            {{ loading ? '提交中...' : isEditing ? '保存修改' : '提交审核' }}
          </button>
          <button
            v-if="isEditing"
            class="btn ghost"
            type="button"
            :disabled="disabled"
            @click="cancelEdit"
          >
            取消编辑
          </button>
        </div>
      </div>
    </section>
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
  }
});

const emit = defineEmits(['submit', 'cancel']);

const steps = [
  { value: 1, label: '基础信息' },
  { value: 2, label: '图片与定价' },
  { value: 3, label: '产权证明' }
];

const listingStatusLabels = {
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
};

const currentStep = ref(1);
const stepErrors = reactive({ 1: '', 2: '', 3: '' });

const sellerRoles = ['SELLER', 'LANDLORD'];
const disabled = computed(() => !props.canManage || props.loading);
const isEditing = computed(() => Boolean(props.initialHouse));
const isSeller = computed(() => sellerRoles.includes(props.currentUser?.role));
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

const createInitialImages = (images = []) => {
  const list = Array.isArray(images) && images.length > 0 ? [...images] : [''];
  return list;
};

const parseKeywords = (value) => {
  if (!value) {
    return [];
  }
  return value
    .split(/[，,、\s]+/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
};

const emptyForm = () => ({
  title: '',
  address: '',
  price: '',
  area: '',
  description: '',
  sellerUsername: isSeller.value ? props.currentUser?.username ?? '' : '',
  sellerName: isSeller.value ? props.currentUser?.displayName ?? '' : '',
  contactNumber: '',
  listingDate: '',
  floor: '',
  keywordInput: '',
  keywords: [],
  installmentMonthlyPayment: '',
  installmentMonths: '',
  propertyCertificateUrl: '',
  imageUrls: createInitialImages()
});

const form = reactive(emptyForm());

watch(
  () => props.initialHouse,
  (house) => {
    currentStep.value = 1;
    if (house) {
      Object.assign(form, {
        title: house.title ?? '',
        address: house.address ?? '',
        price: house.price ?? '',
        area: house.area ?? '',
        description: house.description ?? '',
        sellerUsername: house.sellerUsername ?? '',
        sellerName: house.sellerName ?? '',
        contactNumber: house.contactNumber ?? '',
        listingDate: house.listingDate ?? '',
        floor: house.floor ?? '',
        installmentMonthlyPayment: house.installmentMonthlyPayment ?? '',
        installmentMonths: house.installmentMonths ?? '',
        propertyCertificateUrl: house.propertyCertificateUrl ?? '',
        keywordInput: Array.isArray(house.keywords) ? house.keywords.join('、') : '',
        imageUrls: createInitialImages(house.imageUrls ?? [])
      });
    } else {
      Object.assign(form, emptyForm());
    }
    clearErrors();
  },
  { immediate: true }
);

watch(
  () => props.currentUser,
  () => {
    if (!props.initialHouse) {
      Object.assign(form, emptyForm());
    }
  }
);

const goToStep = (step) => {
  if (step < currentStep.value && step >= 1) {
    currentStep.value = step;
    return;
  }
  if (step > currentStep.value) {
    const valid = validateStep(currentStep.value);
    if (valid) {
      currentStep.value = step;
    }
  }
};

const nextStep = () => {
  if (validateStep(currentStep.value) && currentStep.value < steps.length) {
    currentStep.value += 1;
  }
};

const previousStep = () => {
  if (currentStep.value > 1) {
    currentStep.value -= 1;
  }
};

const clearErrors = () => {
  stepErrors[1] = '';
  stepErrors[2] = '';
  stepErrors[3] = '';
};

const validateStep = (step) => {
  clearErrors();
  if (step === 1) {
    if (!form.title || !form.address || !form.sellerUsername || !form.sellerName || !form.contactNumber || !form.listingDate) {
      stepErrors[1] = '请完整填写必填信息。';
      return false;
    }
    if (!form.area || Number(form.area) <= 0) {
      stepErrors[1] = '请填写有效的房源面积。';
      return false;
    }
  }
  if (step === 2) {
    if (!form.price || Number(form.price) <= 0) {
      stepErrors[2] = '请填写有效的全款价格。';
      return false;
    }
    if (!form.installmentMonthlyPayment || Number(form.installmentMonthlyPayment) <= 0) {
      stepErrors[2] = '请填写有效的分期月供。';
      return false;
    }
    if (!form.installmentMonths || Number(form.installmentMonths) <= 0) {
      stepErrors[2] = '分期期数必须大于0。';
      return false;
    }
  }
  if (step === 3) {
    if (!form.propertyCertificateUrl) {
      stepErrors[3] = '请提供房屋产权证明链接。';
      return false;
    }
  }
  return true;
};

const submitForm = () => {
  if (!validateStep(3)) {
    return;
  }
  if (!props.canManage) {
    return;
  }
  const payload = {
    ...form,
    keywords: parseKeywords(form.keywordInput),
    imageUrls: form.imageUrls
      .map((url) => url?.trim())
      .filter((url) => url && url.length > 0)
  };
  emit('submit', payload);
  if (!isEditing.value) {
    Object.assign(form, emptyForm());
    currentStep.value = 1;
  }
};

const cancelEdit = () => {
  emit('cancel');
  Object.assign(form, emptyForm());
  currentStep.value = 1;
};

const addImageField = () => {
  if (disabled.value) {
    return;
  }
  form.imageUrls.push('');
};

const removeImageField = (index) => {
  if (disabled.value || form.imageUrls.length === 1) {
    return;
  }
  form.imageUrls.splice(index, 1);
};

const formatNumber = (value) => {
  if (value == null || value === '') {
    return '0';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0';
  }
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
};

const keywordsPreview = computed(() => {
  const list = parseKeywords(form.keywordInput);
  return list.length > 0 ? list.join('、') : '未设置';
});
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
  font-size: 0.92rem;
  border: 1px solid rgba(129, 140, 248, 0.35);
}

.status-indicator {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.3rem;
  padding: 0.55rem 0.85rem;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.status-indicator.status.approved {
  background: rgba(34, 197, 94, 0.18);
  color: #166534;
  border-color: rgba(34, 197, 94, 0.32);
}

.status-indicator.status.rejected {
  background: rgba(248, 113, 113, 0.2);
  color: #b91c1c;
  border-color: rgba(248, 113, 113, 0.35);
}

.status-indicator.status.pending {
  background: rgba(251, 191, 36, 0.2);
  color: #92400e;
  border-color: rgba(251, 191, 36, 0.35);
}

.stepper {
  display: flex;
  gap: 0.85rem;
}

.step {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.75rem 1rem;
  border: none;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.7);
  color: var(--color-text-soft);
  font-weight: 600;
  cursor: pointer;
  transition: background var(--transition-base), box-shadow var(--transition-base),
    color var(--transition-base);
  border: 1px solid rgba(148, 163, 184, 0.28);
}

.step .index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.18);
  color: #1e3a8a;
  font-size: 0.85rem;
  font-weight: 700;
}

.step.active {
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 18px 35px rgba(37, 99, 235, 0.25);
  border-color: transparent;
}

.step.active .index {
  background: rgba(255, 255, 255, 0.28);
  color: #fff;
}

.step.done {
  background: rgba(34, 197, 94, 0.18);
  color: #15803d;
  border-color: rgba(34, 197, 94, 0.3);
}

.step-panel {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.step-panel h3 {
  margin: 0;
  color: var(--color-text-strong);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.1rem;
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
  padding: 0.75rem 0.85rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(148, 163, 184, 0.32);
  background: rgba(255, 255, 255, 0.9);
  transition: border-color var(--transition-base), box-shadow var(--transition-base),
    background var(--transition-base);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

input:focus,
textarea:focus {
  outline: none;
  border-color: rgba(37, 99, 235, 0.6);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.25);
  background: rgba(255, 255, 255, 0.98);
}

textarea {
  resize: vertical;
}

.block {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.pricing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
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

.image-inputs {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.image-input {
  display: flex;
  gap: 0.6rem;
  align-items: center;
}

.image-input input {
  flex: 1;
}

.navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.actions {
  display: flex;
  gap: 0.85rem;
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
  background: rgba(226, 232, 240, 0.85);
  color: var(--color-text-strong);
  border: 1px solid rgba(148, 163, 184, 0.35);
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
  border: 1px solid rgba(248, 113, 113, 0.35);
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}

.btn:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.18);
}

.error {
  margin: 0;
  color: #ef4444;
  font-size: 0.9rem;
  background: rgba(254, 226, 226, 0.65);
  border-radius: var(--radius-md);
  padding: 0.6rem 0.9rem;
  border: 1px solid rgba(248, 113, 113, 0.35);
}

.summary {
  background: rgba(248, 250, 252, 0.85);
  border-radius: var(--radius-lg);
  padding: 1.15rem;
  border: 1px solid rgba(226, 232, 240, 0.65);
}

.summary h4 {
  margin: 0 0 0.6rem;
}

.summary ul {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--color-text-muted);
}

@media (max-width: 640px) {
  .stepper {
    flex-direction: column;
  }
}
</style>
