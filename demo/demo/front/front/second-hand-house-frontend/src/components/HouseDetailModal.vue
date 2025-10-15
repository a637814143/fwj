<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal">
      <header class="modal__header">
        <div class="modal__title">
          <h3>{{ house.title }}</h3>
          <p class="modal__address">{{ house.address }}</p>
        </div>
        <button type="button" class="icon-button" aria-label="关闭详情" @click="emit('close')">×</button>
      </header>

      <section class="modal__content">
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">价格</span>
            <span class="info-value">{{ formattedPrice }} 万元</span>
          </div>
          <div class="info-item">
            <span class="info-label">面积</span>
            <span class="info-value">{{ formattedArea }} ㎡</span>
          </div>
          <div class="info-item">
            <span class="info-label">挂牌日期</span>
            <span class="info-value">{{ formattedDate }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">卖家账号</span>
            <span class="info-value">{{ house.sellerUsername || '-' }}</span>
          </div>
        </div>

        <p v-if="house.description" class="modal__description">{{ house.description }}</p>

        <div class="gallery">
          <div class="gallery__header">
            <h4>图片展示</h4>
            <span v-if="images.length" class="gallery__count">共 {{ images.length }} 张</span>
          </div>
          <p v-if="!images.length" class="gallery__empty">暂无上传图片。</p>
          <div v-else class="gallery__grid">
            <figure v-for="(image, index) in images" :key="`${index}-${image.length}`" class="gallery__item">
              <img :src="image" :alt="`${house.title} 图片 ${index + 1}`" loading="lazy" />
              <figcaption>图片 {{ index + 1 }}</figcaption>
            </figure>
          </div>
        </div>
      </section>

      <footer class="modal__footer">
        <div class="contact">
          <span>卖家：{{ house.sellerName }}</span>
          <span>联系方式：{{ house.contactNumber }}</span>
        </div>
        <button type="button" class="primary-button" @click="emit('close')">关闭</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  house: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close']);

const images = computed(() => (Array.isArray(props.house?.imageUrls) ? props.house.imageUrls : []));

const formattedPrice = computed(() => formatNumber(props.house?.price));
const formattedArea = computed(() => formatNumber(props.house?.area));
const formattedDate = computed(() => formatDate(props.house?.listingDate));

function formatNumber(value) {
  if (value == null || value === '') {
    return '-';
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleDateString('zh-CN');
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1.5rem;
  z-index: 1000;
}

.modal {
  width: min(900px, 100%);
  max-height: 90vh;
  background: #fff;
  border-radius: 1.25rem;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.35);
}

.modal__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.5rem 1.75rem;
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  color: #fff;
}

.modal__title h3 {
  margin: 0 0 0.25rem;
  font-size: 1.4rem;
}

.modal__address {
  margin: 0;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.85);
}

.icon-button {
  border: none;
  background: rgba(15, 23, 42, 0.2);
  color: #fff;
  font-size: 1.5rem;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 50%;
  cursor: pointer;
  line-height: 2.25rem;
}

.icon-button:hover {
  background: rgba(15, 23, 42, 0.35);
}

.modal__content {
  padding: 1.5rem 1.75rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.info-item {
  background: #f8fafc;
  border-radius: 0.85rem;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.info-label {
  font-size: 0.85rem;
  color: #64748b;
}

.info-value {
  font-weight: 700;
  color: #1f2937;
}

.modal__description {
  margin: 0;
  line-height: 1.7;
  color: #1f2937;
}

.gallery {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.gallery__header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.gallery__header h4 {
  margin: 0;
  color: #1f2937;
}

.gallery__count {
  font-size: 0.85rem;
  color: #475569;
}

.gallery__empty {
  margin: 0;
  padding: 1rem;
  border-radius: 0.85rem;
  background: #f1f5f9;
  color: #475569;
  text-align: center;
}

.gallery__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.gallery__item {
  background: #0f172a;
  border-radius: 0.85rem;
  overflow: hidden;
  box-shadow: 0 15px 30px rgba(15, 23, 42, 0.25);
  color: #fff;
}

.gallery__item img {
  display: block;
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.gallery__item figcaption {
  margin: 0;
  padding: 0.5rem 0.75rem;
  font-size: 0.85rem;
  background: rgba(15, 23, 42, 0.65);
}

.modal__footer {
  padding: 1.25rem 1.75rem;
  background: #f8fafc;
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
}

.contact {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: #1f2937;
}

.primary-button {
  padding: 0.65rem 1.5rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.primary-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.25);
}

@media (max-width: 640px) {
  .modal__content {
    padding: 1.25rem;
  }

  .modal__header,
  .modal__footer {
    padding: 1.25rem;
  }

  .gallery__grid {
    grid-template-columns: 1fr;
  }

  .gallery__item img {
    height: 220px;
  }
}
</style>
