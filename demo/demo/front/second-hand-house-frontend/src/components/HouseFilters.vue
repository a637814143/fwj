<template>
  <form class="filters" @submit.prevent="applyFilters">
    <div class="grid">
      <label>
        关键词
        <input
          v-model.trim="localFilters.keyword"
          type="text"
          placeholder="标题 / 地址 / 描述"
          :disabled="loading"
        />
      </label>
      <label>
        卖家账号
        <input v-model.trim="localFilters.sellerUsername" type="text" placeholder="seller01" :disabled="loading" />
      </label>
      <label>
        最低价格 (万元)
        <input v-model.number="localFilters.minPrice" type="number" min="0" step="0.01" :disabled="loading" />
      </label>
      <label>
        最高价格 (万元)
        <input v-model.number="localFilters.maxPrice" type="number" min="0" step="0.01" :disabled="loading" />
      </label>
      <label>
        最小面积 (㎡)
        <input v-model.number="localFilters.minArea" type="number" min="0" step="0.01" :disabled="loading" />
      </label>
      <label>
        最大面积 (㎡)
        <input v-model.number="localFilters.maxArea" type="number" min="0" step="0.01" :disabled="loading" />
      </label>
      <label>
        挂牌开始日期
        <input v-model="localFilters.listedFrom" type="date" :disabled="loading" />
      </label>
      <label>
        挂牌截止日期
        <input v-model="localFilters.listedTo" type="date" :disabled="loading" />
      </label>
    </div>
    <div class="actions">
      <button class="btn primary" type="submit" :disabled="loading">{{ loading ? '筛选中...' : '应用筛选' }}</button>
      <button class="btn secondary" type="button" @click="resetFilters" :disabled="loading">重置</button>
    </div>
  </form>
</template>

<script setup>
import { reactive, watch } from 'vue';

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue', 'submit', 'reset']);

const localFilters = reactive({
  keyword: '',
  sellerUsername: '',
  minPrice: '',
  maxPrice: '',
  minArea: '',
  maxArea: '',
  listedFrom: '',
  listedTo: ''
});

watch(
  () => props.modelValue,
  (value) => {
    Object.assign(localFilters, value ?? {});
  },
  { immediate: true }
);

const applyFilters = () => {
  emit('update:modelValue', { ...localFilters });
  emit('submit');
};

const resetFilters = () => {
  Object.assign(localFilters, {
    keyword: '',
    sellerUsername: '',
    minPrice: '',
    maxPrice: '',
    minArea: '',
    maxArea: '',
    listedFrom: '',
    listedTo: ''
  });
  emit('update:modelValue', { ...localFilters });
  emit('reset');
};
</script>

<style scoped>
.filters {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: #fff;
  border-radius: 1rem;
  padding: 1.25rem;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: #1f2937;
  font-weight: 600;
}

input {
  padding: 0.6rem 0.75rem;
  border-radius: 0.65rem;
  border: 1px solid #cbd5f5;
  background: #f8fafc;
}

.actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.btn {
  padding: 0.6rem 1.4rem;
  border-radius: 0.75rem;
  border: none;
  font-weight: 600;
}

.btn.primary {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
}

.btn.secondary {
  background: #e2e8f0;
  color: #1e293b;
}

.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
