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

    <div class="image-section">
      <div class="image-header">
        <span>房源图片链接</span>
        <small>支持粘贴网络图片地址，最多 10 张。</small>
      </div>
      <div class="image-inputs">
        <div class="image-input-row" v-for="(image, index) in form.imageUrls" :key="index">
          <input
            v-model.trim="form.imageUrls[index]"
            type="url"
            placeholder="https://example.com/house.jpg"
            :disabled="!canManage || loading"
          />
          <button
            v-if="form.imageUrls.length > 1"
            type="button"
            class="btn tertiary"
            @click="removeImageField(index)"
            :disabled="!canManage || loading"
          >
            移除
          </button>
        </div>
        <button
          type="button"
          class="btn secondary"
          @click="addImageField"
          :disabled="!canManage || loading || form.imageUrls.length >= 10"
        >
          添加图片
        </button>
      </div>
    </div>

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
  imageUrls: ['']
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
        imageUrls: Array.isArray(house.imageUrls) && house.imageUrls.length > 0 ? [...house.imageUrls] : ['']
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
      .filter((url) => url)
  };
  emit('submit', payload);
  form.imageUrls = payload.imageUrls.length > 0 ? [...payload.imageUrls] : [''];
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
  if (form.imageUrls.length >= 10) {
    return;
  }
  form.imageUrls.push('');
};

const removeImageField = (index) => {
  if (form.imageUrls.length <= 1) {
    form.imageUrls[0] = '';
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

.btn.tertiary {
  background: #fee2e2;
  color: #b91c1c;
}

.image-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.image-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-weight: 600;
  color: #1f2937;
}

.image-header small {
  font-weight: 400;
  color: #64748b;
}

.image-inputs {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.image-input-row {
  display: flex;
  gap: 0.5rem;
}

.image-input-row input {
  flex: 1;
}
</style>
