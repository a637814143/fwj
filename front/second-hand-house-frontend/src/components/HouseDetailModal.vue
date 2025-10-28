<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal">
      <header class="modal__header">
        <div class="modal__title">
          <h3>{{ house.title }}</h3>
          <p class="modal__address">{{ house.address }}</p>
        </div>
        <button type="button" class="icon-button" aria-label="Close details" @click="emit('close')">×</button>
      </header>

      <section class="modal__content">
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">Price</span>
            <span class="info-value">¥{{ formattedPrice }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Down payment</span>
            <span class="info-value">¥{{ formattedDownPayment }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Installment plan</span>
            <span class="info-value">
              {{ formattedInstallment }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">Area</span>
            <span class="info-value">{{ formattedArea }} ㎡</span>
          </div>
          <div class="info-item">
            <span class="info-label">Listed on</span>
            <span class="info-value">{{ formattedDate }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Floor</span>
            <span class="info-value">{{ formattedFloor }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Status</span>
            <span class="info-value">
              <span :class="['status-chip', statusClass]">{{ statusLabel }}</span>
            </span>
          </div>
        </div>

        <p v-if="house.description" class="modal__description">{{ house.description }}</p>

        <div v-if="keywordList.length" class="keywords">
          <h4>Listing keywords</h4>
          <ul>
            <li v-for="keyword in keywordList" :key="keyword">{{ keyword }}</li>
          </ul>
        </div>

        <div class="gallery">
          <div class="gallery__header">
            <h4>Gallery</h4>
            <span v-if="images.length" class="gallery__count">{{ images.length }} images</span>
          </div>
          <p v-if="!images.length" class="gallery__empty">No images uploaded yet.</p>
          <div v-else class="gallery__grid">
            <figure v-for="(image, index) in images" :key="`${index}-${image}`" class="gallery__item">
              <img :src="image" :alt="`${house.title} image ${index + 1}`" loading="lazy" />
              <figcaption>Image {{ index + 1 }}</figcaption>
            </figure>
          </div>
        </div>
      </section>

      <footer class="modal__footer">
        <div class="contact">
          <span>Seller: {{ sellerName }}</span>
          <span>Contact: {{ maskedPhone }}</span>
        </div>
        <button type="button" class="primary-button" @click="emit('close')">Close</button>
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
  },
  canViewSensitiveInfo: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['close']);

const images = computed(() => (Array.isArray(props.house?.imageUrls) ? props.house.imageUrls : []));

const formattedPrice = computed(() => formatCurrency(props.house?.price));
const formattedDownPayment = computed(() => formatCurrency(props.house?.downPayment));
const formattedInstallment = computed(() => {
  const monthly = formatCurrency(props.house?.installmentMonthlyPayment);
  const months =
    props.house?.installmentMonths != null && props.house.installmentMonths !== ''
      ? props.house.installmentMonths
      : '—';
  if (monthly === '—') {
    return '—';
  }
  return `¥${monthly} × ${months} months`;
});
const formattedArea = computed(() => formatNumber(props.house?.area));
const formattedDate = computed(() => formatDate(props.house?.listingDate));
const formattedFloor = computed(() => {
  if (props.house?.floor == null || props.house.floor === '') {
    return '—';
  }
  return `Floor ${props.house.floor}`;
});

const listingStatusLabels = {
  PENDING_REVIEW: 'Pending review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  SOLD: 'Sold (unlisted)'
};

const statusLabel = computed(() => listingStatusLabels[props.house?.status] ?? 'Pending review');
const statusClass = computed(() => {
  switch (props.house?.status) {
    case 'APPROVED':
      return 'approved';
    case 'REJECTED':
      return 'rejected';
    case 'SOLD':
      return 'sold';
    default:
      return 'pending';
  }
});

const keywordList = computed(() => (Array.isArray(props.house?.keywords) ? props.house.keywords : []));

const sellerName = computed(() => {
  if (props.canViewSensitiveInfo) {
    return props.house?.sellerName ?? '—';
  }
  return maskName(props.house?.sellerName);
});

const maskedPhone = computed(() => {
  const phone = props.house?.contactNumber;
  if (props.canViewSensitiveInfo) {
    return phone ?? '—';
  }
  return maskPhone(phone);
});

function formatNumber(value) {
  if (value == null || value === '') {
    return '—';
  }
  return Number(value).toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
}

function formatCurrency(value) {
  if (value == null || value === '') {
    return '—';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '—';
  }
  return num.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleDateString('en-US');
}

function maskPhone(value) {
  if (!value) {
    return '-';
  }
  const phone = String(value).trim();
  if (phone.length <= 4) {
    return '*'.repeat(phone.length);
  }
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`;
}

function maskName(value) {
  if (!value) {
    return '—';
  }
  const text = String(value);
  if (text.length === 1) {
    return `${text}*`;
  }
  return `${text.slice(0, 1)}**`;
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
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  background: rgba(248, 250, 252, 0.8);
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
}

.info-label {
  font-size: 0.85rem;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  font-size: 1rem;
  color: #1e293b;
  font-weight: 600;
}

.modal__description {
  margin: 0;
  font-size: 1rem;
  color: #475569;
  line-height: 1.6;
}

.keywords ul {
  margin: 0.5rem 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.keywords li {
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-weight: 600;
}

.gallery__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.gallery__count {
  color: #475569;
  font-size: 0.9rem;
}

.gallery__empty {
  margin: 0;
  color: #94a3b8;
}

.gallery__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.9rem;
}

.gallery__item {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  background: rgba(248, 250, 252, 0.8);
  padding: 0.75rem;
  border-radius: 0.85rem;
  align-items: center;
}

.gallery__item img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
}

.modal__footer {
  padding: 1.35rem 1.75rem;
  border-top: 1px solid rgba(226, 232, 240, 0.8);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.contact {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: #1f2937;
}

.primary-button {
  padding: 0.75rem 1.6rem;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-chip.pending {
  background: rgba(250, 204, 21, 0.18);
  color: #92400e;
}

.status-chip.approved {
  background: rgba(34, 197, 94, 0.18);
  color: #166534;
}

.status-chip.rejected {
  background: rgba(239, 68, 68, 0.2);
  color: #991b1b;
}

.status-chip.sold {
  background: rgba(148, 163, 184, 0.2);
  color: var(--color-text-strong);
}

:global(body[data-theme='dark']) :deep(.status-chip.pending) {
  background: rgba(253, 224, 71, 0.18);
  color: #facc15;
}

:global(body[data-theme='dark']) :deep(.status-chip.approved) {
  background: rgba(74, 222, 128, 0.18);
  color: #bbf7d0;
}

:global(body[data-theme='dark']) :deep(.status-chip.rejected) {
  background: rgba(248, 113, 113, 0.2);
  color: #fecaca;
}

:global(body[data-theme='dark']) :deep(.status-chip.sold) {
  background: rgba(148, 163, 184, 0.26);
  color: rgba(226, 232, 240, 0.92);
}

@media (max-width: 768px) {
  .modal__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .contact {
    width: 100%;
  }

  .primary-button {
    width: 100%;
    text-align: center;
  }
}
</style>
