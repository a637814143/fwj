<template>
  <form class="house-form" @submit.prevent="submitForm">
    <h2>{{ isEditing ? '编辑房源' : '新增房源' }}</h2>

    <div class="form-grid">
      <label>
        标题
        <input v-model.trim="form.title" type="text" required placeholder="请输入房源标题" />
      </label>

      <label>
        地址
        <input v-model.trim="form.address" type="text" required placeholder="请输入房源地址" />
      </label>

      <label>
        价格（万元）
        <input v-model.number="form.price" type="number" min="0" step="0.01" required placeholder="例如 200" />
      </label>

      <label>
        面积（㎡）
        <input v-model.number="form.area" type="number" min="0" step="0.01" required placeholder="例如 89" />
      </label>

      <label>
        卖家姓名
        <input v-model.trim="form.sellerName" type="text" required placeholder="请输入卖家姓名" />
      </label>

      <label>
        联系方式
        <input v-model.trim="form.contactNumber" type="text" required placeholder="请输入联系方式" />
      </label>

      <label>
        挂牌日期
        <input v-model="form.listingDate" type="date" required />
      </label>
    </div>

    <label class="description">
      房源描述
      <textarea v-model.trim="form.description" rows="3" placeholder="补充房源亮点或其他信息"></textarea>
    </label>

    <div class="actions">
      <button class="btn primary" type="submit" :disabled="loading">
        {{ loading ? '提交中...' : isEditing ? '保存修改' : '添加房源' }}
      </button>
      <button v-if="isEditing" class="btn secondary" type="button" @click="cancelEdit" :disabled="loading">
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
  }
});

const emit = defineEmits(['submit', 'cancel']);

const emptyForm = () => ({
  title: '',
  address: '',
  price: '',
  area: '',
  description: '',
  sellerName: '',
  contactNumber: '',
  listingDate: ''
});

const form = reactive(emptyForm());

const isEditing = computed(() => Boolean(props.initialHouse));

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
        sellerName: house.sellerName ?? '',
        contactNumber: house.contactNumber ?? '',
        listingDate: house.listingDate ?? ''
      });
    } else {
      Object.assign(form, emptyForm());
    }
  },
  { immediate: true }
);

const submitForm = () => {
  emit('submit', { ...form });
  if (!isEditing.value) {
    Object.assign(form, emptyForm());
  }
};

const cancelEdit = () => {
  emit('cancel');
  Object.assign(form, emptyForm());
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
</style>
