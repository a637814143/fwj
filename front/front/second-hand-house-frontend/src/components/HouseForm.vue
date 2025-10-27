<template>
  <form class="house-form" @submit.prevent="submitForm">
    <h2>{{ isEditing ? '编辑房源' : '新增房源' }}</h2>
    <p v-if="!canManage" class="notice">当前角色仅支持浏览房源，如需发布或维护房源请使用卖家或系统管理员账号。</p>

    <div class="form-grid">
      <label>
        标题
        <input
          v-model.trim="form.title"
          type="text"
          required
          placeholder="请输入房源标题"
          :disabled="!canManage || loading"
        />
      </label>

      <label>
        地址
        <input
          v-model.trim="form.address"
          type="text"
          required
          placeholder="请输入房源地址"
          :disabled="!canManage || loading"
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
          placeholder="例如 200"
          :disabled="!canManage || loading"
        />
      </label>

      <label>
        面积（㎡）
        <input
          v-model.number="form.area"
          type="number"
          min="0"
          step="0.01"
          required
          placeholder="例如 89"
          :disabled="!canManage || loading"
        />
      </label>

      <label>
        楼层
        <input
          v-model.number="form.floor"
          type="number"
          min="0"
          step="1"
          placeholder="请输入楼层，如 12"
          :disabled="!canManage || loading"
        />
      </label>

      <label>
        卖家账号
        <input
          v-model.trim="form.sellerUsername"
          type="text"
          required
          placeholder="请输入卖家账号"
          :disabled="!canManage || loading || disableSellerAccount"
        />
      </label>

      <label>
        卖家姓名
        <input
          v-model.trim="form.sellerName"
          type="text"
          required
          placeholder="请输入卖家姓名"
          :disabled="!canManage || loading"
        />
      </label>

      <label>
        联系方式
        <input
          v-model.trim="form.contactNumber"
          type="text"
          required
          placeholder="请输入联系方式"
          :disabled="!canManage || loading"
        />
      </label>

      <label>
        关键词（使用逗号或顿号分隔）
        <input
          v-model.trim="keywordsText"
          type="text"
          placeholder="例如：靠近学校，靠近医院"
          :disabled="!canManage || loading"
          @change="syncKeywords"
          @blur="syncKeywords"
        />
      </label>

      <label>
        挂牌日期
        <input v-model="form.listingDate" type="date" required :disabled="!canManage || loading" />
      </label>
    </div>

    <label class="description">
      房源描述
      <textarea
        v-model.trim="form.description"
        rows="3"
        placeholder="补充房源亮点或其他信息"
        :disabled="!canManage || loading"
      ></textarea>
    </label>

    <section class="image-section">
      <div class="image-section__header">
        <span class="section-title">房源图片</span>
        <span class="image-helper">最多 {{ MAX_IMAGES }} 张，建议尺寸 4:3</span>
      </div>
      <input
        class="upload-input"
        type="file"
        accept="image/*"
        multiple
        :disabled="!canManage || loading || imageUploading || !canUploadMore"
        @change="handleImageUpload"
      />
      <p v-if="imageUploadError" class="image-error">{{ imageUploadError }}</p>
      <p v-else class="image-hint">
        {{ imageUploading ? '图片处理中，请稍候…' : `还可上传 ${remainingSlots} 张图片。` }}
      </p>
      <ul v-if="form.imageUrls.length" class="image-preview">
        <li v-for="(image, index) in form.imageUrls" :key="`${index}-${image.length}`" class="image-preview__item">
          <img :src="image" :alt="`房源图片 ${index + 1}`" loading="lazy" />
          <button
            type="button"
            class="remove-btn"
            :disabled="!canManage || loading"
            @click="removeImage(index)"
          >
            移除
          </button>
        </li>
      </ul>
    </section>

    <div class="actions">
      <button class="btn primary" type="submit" :disabled="loading || !canManage || imageUploading">
        {{ loading ? '提交中...' : isEditing ? '保存修改' : '添加房源' }}
      </button>
      <button
        v-if="isEditing"
        class="btn secondary"
        type="button"
        @click="cancelEdit"
        :disabled="loading || !canManage"
      >
        取消编辑
      </button>
    </div>
  </form>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';

const MAX_IMAGES = 5;

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
  keywords: [],
  imageUrls: []
});

const form = reactive(emptyForm());

const isEditing = computed(() => Boolean(props.initialHouse));
const canManage = computed(() => props.canManage);
const loading = computed(() => props.loading);
const disableSellerAccount = computed(() => props.currentUser?.role === 'SELLER');
const imageUploadError = ref('');
const imageUploading = ref(false);
const remainingSlots = computed(() => Math.max(0, MAX_IMAGES - form.imageUrls.length));
const canUploadMore = computed(() => remainingSlots.value > 0);
const keywordsText = ref('');

const resetForm = () => {
  Object.assign(form, emptyForm());
  imageUploadError.value = '';
  keywordsText.value = '';
};

watch(
  () => props.initialHouse,
  (house) => {
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
        keywords: Array.isArray(house.keywords) ? [...house.keywords] : [],
        imageUrls: Array.isArray(house.imageUrls) ? [...house.imageUrls] : []
      });
      keywordsText.value = form.keywords.join('、');
      imageUploadError.value = '';
    } else {
      resetForm();
    }
  },
  { immediate: true }
);

watch(
  () => props.currentUser,
  () => {
    if (!props.initialHouse) {
      resetForm();
    }
  }
);

const syncKeywords = () => {
  if (typeof keywordsText.value !== 'string') {
    form.keywords = [];
    return;
  }
  form.keywords = keywordsText.value
    .split(/[、,，\s]+/)
    .map((item) => item.trim())
    .filter((item) => item);
};

const readFileAsDataUrl = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = (event) => reject(event);
    reader.readAsDataURL(file);
  });

const handleImageUpload = async (event) => {
  const input = event.target;
  if (!canManage.value || loading.value) {
    input.value = '';
    return;
  }

  const files = Array.from(input.files ?? []);
  if (!files.length) {
    input.value = '';
    return;
  }

  if (!canUploadMore.value) {
    imageUploadError.value = `最多只能上传 ${MAX_IMAGES} 张图片`;
    input.value = '';
    return;
  }

  const filesToProcess = files.slice(0, remainingSlots.value);
  imageUploading.value = true;
  imageUploadError.value = '';

  try {
    const images = await Promise.all(filesToProcess.map((file) => readFileAsDataUrl(file)));
    form.imageUrls.push(...images);
  } catch (error) {
    console.error('图片读取失败：', error);
    imageUploadError.value = '读取图片失败，请重试。';
  } finally {
    imageUploading.value = false;
    input.value = '';
  }
};

const removeImage = (index) => {
  if (!canManage.value || loading.value) {
    return;
  }
  form.imageUrls.splice(index, 1);
};

const submitForm = () => {
  if (!canManage.value || imageUploading.value) {
    return;
  }
  syncKeywords();
  emit('submit', {
    ...form,
    floor: form.floor === '' || form.floor == null ? null : Number(form.floor),
    keywords: [...form.keywords],
    imageUrls: [...form.imageUrls]
  });
  if (!isEditing.value) {
    resetForm();
  }
};

const cancelEdit = () => {
  if (!canManage.value) {
    return;
  }
  emit('cancel');
  resetForm();
};
</script>

<style scoped>
.house-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  color: var(--text-primary);
}

.house-form h2 {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 600;
}

.notice {
  margin: 0;
  padding: 0.85rem 1.1rem;
  background: rgba(96, 165, 250, 0.18);
  border-left: 4px solid rgba(96, 165, 250, 0.55);
  border-radius: 1rem;
  color: #dbeafe;
  font-size: 0.95rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

label {
  display: flex;
  flex-direction: column;
  font-weight: 600;
  color: var(--text-secondary);
  gap: 0.4rem;
}

input,
textarea {
  padding: 0.7rem 0.85rem;
  border-radius: 0.85rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(15, 23, 42, 0.65);
  color: var(--text-primary);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

input:focus,
textarea:focus {
  outline: none;
  border-color: rgba(96, 165, 250, 0.8);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.25);
}

textarea {
  resize: vertical;
  min-height: 120px;
}

.image-section {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  background: rgba(15, 23, 42, 0.55);
  border-radius: 1.25rem;
  padding: 1.15rem;
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.image-section__header {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.section-title {
  font-weight: 700;
  color: var(--text-primary);
}

.image-helper {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.upload-input {
  padding: 0.65rem;
  border-radius: 0.85rem;
  border: 1px dashed rgba(148, 163, 184, 0.45);
  background: rgba(15, 23, 42, 0.65);
  color: var(--text-secondary);
  cursor: pointer;
}

.upload-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.image-hint {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.image-error {
  margin: 0;
  font-size: 0.85rem;
  color: var(--danger);
}

.image-preview {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.85rem;
  padding: 0;
  margin: 0;
}

.image-preview__item {
  position: relative;
  border-radius: 0.9rem;
  overflow: hidden;
  box-shadow: 0 18px 30px rgba(8, 15, 35, 0.35);
  background: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.image-preview__item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.remove-btn {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  padding: 0.3rem 0.65rem;
  border-radius: 999px;
  border: none;
  font-size: 0.75rem;
  font-weight: 600;
  background: rgba(239, 68, 68, 0.9);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 10px 18px rgba(239, 68, 68, 0.3);
}

.remove-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.btn {
  padding: 0.75rem 1.6rem;
  border-radius: 0.9rem;
  border: none;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn.primary {
  background: linear-gradient(135deg, var(--accent), rgba(56, 189, 248, 0.8));
  color: var(--text-primary);
  box-shadow: 0 18px 32px rgba(37, 99, 235, 0.35);
}

.btn.primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 20px 36px rgba(37, 99, 235, 0.45);
}

.btn.primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn.secondary {
  background: rgba(148, 163, 184, 0.25);
  color: var(--text-primary);
}
</style>
