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
  gap: 1rem;
}

.house-form h2 {
  margin: 0;
  color: #1e293b;
}

.notice {
  margin: 0;
  padding: 0.75rem 1rem;
  background: #f8fafc;
  border-left: 4px solid #2563eb;
  border-radius: 0.75rem;
  color: #1d4ed8;
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
  color: #1f2937;
  gap: 0.35rem;
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

.image-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  background: #f8fafc;
  border-radius: 1rem;
  padding: 1rem;
}

.image-section__header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.section-title {
  font-weight: 700;
  color: #1f2937;
}

.image-helper {
  font-size: 0.85rem;
  color: #64748b;
}

.upload-input {
  padding: 0.6rem;
  border-radius: 0.65rem;
  border: 1px dashed #94a3b8;
  background: #fff;
  cursor: pointer;
}

.upload-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.image-hint {
  margin: 0;
  font-size: 0.85rem;
  color: #475569;
}

.image-error {
  margin: 0;
  font-size: 0.85rem;
  color: #dc2626;
}

.image-preview {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.75rem;
  padding: 0;
  margin: 0;
}

.image-preview__item {
  position: relative;
  border-radius: 0.75rem;
  overflow: hidden;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.08);
  background: #fff;
}

.image-preview__item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.remove-btn {
  position: absolute;
  top: 0.4rem;
  right: 0.4rem;
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  border: none;
  font-size: 0.75rem;
  font-weight: 600;
  background: rgba(220, 38, 38, 0.9);
  color: #fff;
  cursor: pointer;
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
  padding: 0.75rem 1.5rem;
  border-radius: 0.75rem;
  border: none;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn.primary {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.3);
}

.btn.primary:hover:not(:disabled) {
  transform: translateY(-1px);
}

.btn.primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn.secondary {
  background: #e2e8f0;
  color: #1e293b;
}
</style>
