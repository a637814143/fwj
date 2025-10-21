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

const disabled = computed(() => !props.canManage || props.loading);
const isEditing = computed(() => Boolean(props.initialHouse));
const disableSellerAccount = computed(() => props.currentUser?.role === 'SELLER');
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
  sellerUsername: props.currentUser?.role === 'SELLER' ? props.currentUser.username ?? '' : '',
  sellerName: props.currentUser?.role === 'SELLER' ? props.currentUser.displayName ?? '' : '',
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
  gap: 1.25rem;
  background: #fff;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.form-header h2 {
  margin: 0;
  color: #1e293b;
}

.notice {
  margin: 0.25rem 0 0;
  padding: 0.5rem 0.75rem;
  background: #eef2ff;
  border-radius: 0.75rem;
  color: #4338ca;
  font-size: 0.9rem;
}

.status-indicator {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
  padding: 0.5rem 0.75rem;
  border-radius: 0.75rem;
  font-size: 0.85rem;
  font-weight: 600;
}

.status-indicator.status.approved {
  background: #dcfce7;
  color: #166534;
}

.status-indicator.status.rejected {
  background: #fee2e2;
  color: #b91c1c;
}

.status-indicator.status.pending {
  background: #fef9c3;
  color: #92400e;
}

.stepper {
  display: flex;
  gap: 0.75rem;
}

.step {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 0.85rem;
  border: none;
  border-radius: 0.75rem;
  background: #f1f5f9;
  color: #475569;
  font-weight: 600;
  cursor: pointer;
}

.step .index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #cbd5f5;
  color: #1e3a8a;
  font-size: 0.85rem;
}

.step.active {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
}

.step.active .index {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.step.done {
  background: #dcfce7;
  color: #15803d;
}

.step-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-weight: 600;
  color: #1f2937;
}

input,
textarea {
  padding: 0.65rem 0.75rem;
  border-radius: 0.65rem;
  border: 1px solid #cbd5f5;
  background: #f8fafc;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

input:focus,
textarea:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
}

textarea {
  resize: vertical;
}

.block {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.pricing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.images-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px dashed #cbd5f5;
  border-radius: 0.75rem;
  background: #f9fbff;
}

.images-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.images-header h4 {
  margin: 0;
  font-size: 1rem;
  color: #1f2937;
}

.image-inputs {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.image-input {
  display: flex;
  gap: 0.5rem;
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
  gap: 0.75rem;
}

.btn {
  padding: 0.65rem 1.2rem;
  border-radius: 0.75rem;
  border: none;
  font-weight: 600;
  cursor: pointer;
}

.btn.primary {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.3);
}

.btn.secondary {
  background: #e2e8f0;
  color: #1e293b;
}

.btn.ghost {
  background: transparent;
  color: #1e293b;
  border: 1px solid #cbd5f5;
}

.btn.add {
  background: #2563eb;
  color: #fff;
  padding: 0.4rem 0.8rem;
}

.btn.remove {
  background: #fee2e2;
  color: #b91c1c;
  padding: 0.4rem 0.8rem;
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.error {
  margin: 0;
  color: #ef4444;
  font-size: 0.9rem;
}

.summary {
  background: #f8fafc;
  border-radius: 0.75rem;
  padding: 1rem;
  box-shadow: inset 0 0 0 1px #e2e8f0;
}

.summary h4 {
  margin: 0 0 0.5rem;
}

.summary ul {
  margin: 0;
  padding-left: 1.1rem;
  color: #475569;
}

@media (max-width: 640px) {
  .stepper {
    flex-direction: column;
  }
}
</style>
