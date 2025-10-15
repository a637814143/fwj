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
        价格（万元）
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

    <section class="images-section">
      <div class="images-header">
        <h3>房源图片</h3>
        <button
          type="button"
          class="btn add-image"
          @click="addImageField"
          :disabled="!canManage || loading"
        >
          添加图片链接
        </button>
      </div>
      <p class="hint">支持填写多张图片的网络链接，提交前会自动忽略空白链接。</p>
      <div class="image-inputs">
        <div v-for="(image, index) in form.imageUrls" :key="index" class="image-input">
          <input
            v-model.trim="form.imageUrls[index]"
            type="url"
            placeholder="例如：https://example.com/house.jpg"
            :disabled="!canManage || loading"
          />
          <button
            type="button"
            class="btn remove-image"
            @click="removeImageField(index)"
            :disabled="!canManage || loading || form.imageUrls.length === 1"
          >
            删除
          </button>
        </div>
      </div>
    </section>

    <div class="actions">
      <button class="btn primary" type="submit" :disabled="loading || !canManage">
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
import { computed, reactive, watch } from 'vue';

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

const createInitialImages = (images = []) => {
  const list = Array.isArray(images) && images.length > 0 ? [...images] : [''];
  return list;
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
  imageUrls: createInitialImages()
});

const form = reactive(emptyForm());

const isEditing = computed(() => Boolean(props.initialHouse));
const canManage = computed(() => props.canManage);
const disableSellerAccount = computed(() => props.currentUser?.role === 'SELLER');

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
        imageUrls: createInitialImages(house.imageUrls ?? [])
      });
    } else {
      Object.assign(form, emptyForm());
    }
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

const submitForm = () => {
  if (!canManage.value) {
    return;
  }
  const payload = {
    ...form,
    imageUrls: form.imageUrls
      .map((url) => url?.trim())
      .filter((url) => url && url.length > 0)
  };
  emit('submit', payload);
  if (!isEditing.value) {
    Object.assign(form, emptyForm());
  }
};

const cancelEdit = () => {
  if (!canManage.value) {
    return;
  }
  emit('cancel');
  Object.assign(form, emptyForm());
};

const addImageField = () => {
  if (!canManage.value) {
    return;
  }
  form.imageUrls.push('');
};

const removeImageField = (index) => {
  if (!canManage.value || form.imageUrls.length === 1) {
    return;
  }
  form.imageUrls.splice(index, 1);
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

.images-header h3 {
  margin: 0;
  font-size: 1rem;
  color: #1f2937;
}

.hint {
  margin: 0;
  color: #64748b;
  font-size: 0.85rem;
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

.btn.add-image {
  padding: 0.4rem 0.8rem;
  background: #2563eb;
  color: #fff;
}

.btn.remove-image {
  padding: 0.4rem 0.8rem;
  background: #fee2e2;
  color: #b91c1c;
}
</style>
