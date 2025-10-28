<template>
  <div class="house-list">
    <div class="list-header">
      <div>
        <h2>{{ t('manage.list.heading') }}</h2>
        <p>{{ t('manage.list.total', { count: houses.length }) }}</p>
      </div>
      <span v-if="!canManage && !isAdmin" class="hint">{{ t('manage.list.notice') }}</span>
    </div>

    <div v-if="loading" class="loading">{{ t('manage.list.loading') }}</div>
    <div v-else-if="houses.length === 0" class="empty">
      {{ canManage ? t('manage.list.emptyManage') : t('manage.list.emptyView') }}
    </div>

    <div v-else class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>{{ t('manage.list.headers.listing') }}</th>
            <th>{{ t('manage.list.headers.pricing') }}</th>
            <th>{{ t('manage.list.headers.area') }}</th>
            <th>{{ t('manage.list.headers.floor') }}</th>
            <th>{{ t('manage.list.headers.date') }}</th>
            <th>{{ t('manage.list.headers.status') }}</th>
            <th>{{ t('manage.list.headers.seller') }}</th>
            <th>{{ t('manage.list.headers.contact') }}</th>
            <th>{{ t('manage.list.headers.keywords') }}</th>
            <th>{{ t('manage.list.headers.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="house in houses" :key="house.id">
            <td class="title-cell">
              <button class="title-button" type="button" @click="openDetail(house)">
                <strong>{{ house.title }}</strong>
              </button>
              <p class="description" v-if="house.description">{{ house.description }}</p>
              <p class="image-count" v-if="house.imageUrls?.length">
                {{ t('manage.list.photoCount', { count: house.imageUrls.length }) }}
              </p>
            </td>
            <td class="price-cell">
              <span>{{ t('manage.list.pricing.full', { price: formatCurrency(house.price) }) }}</span>
              <span v-if="house.downPayment">
                {{ t('manage.list.pricing.downPayment', { amount: formatCurrency(house.downPayment) }) }}
              </span>
              <span v-if="house.installmentMonthlyPayment">
                {{
                  t('manage.list.pricing.installment', {
                    amount: formatCurrency(house.installmentMonthlyPayment),
                    months: house.installmentMonths || '—'
                  })
                }}
              </span>
            </td>
            <td>{{ formatNumber(house.area) }} ㎡</td>
            <td>{{ formatFloor(house.floor) }}</td>
            <td>{{ formatDate(house.listingDate) }}</td>
            <td>
              <span :class="['status', statusClass(house)]">{{ statusLabel(house) }}</span>
              <small v-if="house.reviewMessage" class="status-tip">{{ house.reviewMessage }}</small>
            </td>
            <td>
              <div>{{ sellerUsernameDisplay(house) }}</div>
              <div class="muted">{{ sellerNameDisplay(house) }}</div>
            </td>
            <td>{{ contactNumberDisplay(house) }}</td>
            <td>
              <span v-if="formatKeywords(house.keywords)">{{ formatKeywords(house.keywords) }}</span>
              <span v-else class="muted">{{ t('manage.list.keywordPlaceholder') }}</span>
            </td>
            <td class="actions">
              <template v-if="canManage">
                <button class="btn" :disabled="!canEditHouse(house)" @click="handleEdit(house)">
                  {{ t('manage.list.actions.edit') }}
                </button>
                <button class="btn danger" :disabled="!canDeleteHouse(house)" @click="handleRemove(house)">
                  {{ t('manage.list.actions.delete') }}
                </button>
              </template>
              <template v-else-if="isAdmin">
                <button class="btn success" @click="handleReview(house, 'APPROVED')">
                  {{ t('manage.list.actions.approve') }}
                </button>
                <button class="btn warning" @click="handleReview(house, 'REJECTED')">
                  {{ t('manage.list.actions.reject') }}
                </button>
              </template>
              <template v-else-if="isBuyer">
                <div class="payment-select">
                  <label>
                    {{ t('manage.list.actions.paymentLabel') }}
                    <select v-model="selectedPayments[house.id]">
                      <option value="FULL">{{ t('manage.list.actions.paymentFull') }}</option>
                      <option value="INSTALLMENT" :disabled="!house.installmentMonthlyPayment">
                        {{ t('manage.list.actions.paymentInstallment') }}
                      </option>
                    </select>
                  </label>
                </div>
                <button
                  class="btn primary"
                  :disabled="ordersLoading || loading || house.status !== 'APPROVED'"
                  @click="handlePurchase(house)"
                >
                  {{
                    house.status !== 'APPROVED'
                      ? house.status === 'SOLD'
                        ? t('manage.list.actions.sold')
                        : t('statuses.pending')
                      : ordersLoading
                      ? t('manage.list.actions.processing')
                      : t('manage.list.actions.purchase')
                  }}
                </button>
                <button
                  class="btn ghost"
                  :disabled="ordersLoading || loading || house.status !== 'APPROVED'"
                  @click="handleContactSeller(house)"
                >
                  {{ house.status === 'SOLD' ? t('manage.list.actions.sold') : t('manage.list.actions.contact') }}
                </button>
              </template>
              <span v-else class="muted">{{ t('manage.list.actions.viewOnly') }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

      <HouseDetailModal
        v-if="detailHouse"
        :house="detailHouse"
        :can-view-sensitive-info="detailCanViewSensitive"
        @close="closeDetail"
      />
  </div>
</template>

<script setup>
import { computed, inject, reactive, ref, toRefs, watch } from 'vue';
import HouseDetailModal from './HouseDetailModal.vue';

const props = defineProps({
  houses: {
    type: Array,
    default: () => []
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
  },
  canViewSensitiveInfo: {
    type: Boolean,
    default: false
  },
  ordersLoading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['edit', 'remove', 'purchase', 'contact-seller', 'review']);

const { houses, loading, canManage } = toRefs(props);

const translate = inject('translate', (key, vars) => {
  if (typeof key !== 'string') {
    return '';
  }
  if (!vars) {
    return key;
  }
  return key;
});

const t = (key, vars) => translate(key, vars);

const settings = inject('appSettings', { language: 'zh' });
const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));

const selectedPayments = reactive({});

const sellerRoles = ['SELLER', 'LANDLORD'];
const isBuyer = computed(() => props.currentUser?.role === 'BUYER');
const isAdmin = computed(() => props.currentUser?.role === 'ADMIN');
const isSeller = computed(() => sellerRoles.includes(props.currentUser?.role));
const detailHouse = ref(null);
const detailCanViewSensitive = ref(false);

const listingStatusLabels = computed(() => ({
  PENDING_REVIEW: t('statuses.pending'),
  APPROVED: t('statuses.approved'),
  REJECTED: t('statuses.rejected'),
  SOLD: t('statuses.sold')
}));

const formatNumber = (value) => {
  if (value == null || value === '') {
    return '-';
  }
  return Number(value).toLocaleString(locale.value, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
};

const formatCurrency = (value) => {
  if (value == null || value === '') {
    return '0.00';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  return num.toLocaleString(locale.value, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const formatDate = (value) => {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleDateString(locale.value);
};

const formatFloor = (value) => {
  if (value == null || value === '') {
    return '—';
  }
  return t('manage.list.floorValue', { floor: value });
};

const formatKeywords = (keywords) => {
  if (!Array.isArray(keywords) || keywords.length === 0) {
    return '';
  }
  const separator = t('manage.list.keywordSeparator');
  const joiner = typeof separator === 'string' && separator.length ? separator : '、';
  return keywords.join(joiner);
};

const maskName = (value) => {
  if (!value) {
    return '—';
  }
  const text = String(value);
  if (text.length === 1) {
    return `${text}*`;
  }
  return `${text.slice(0, 1)}**`;
};

const maskUsername = (value) => {
  if (!value) {
    return '—';
  }
  const text = String(value);
  if (text.length <= 2) {
    return '*'.repeat(text.length);
  }
  return `${text.slice(0, 1)}${'*'.repeat(text.length - 2)}${text.slice(-1)}`;
};

const maskPhone = (value) => {
  if (!value) {
    return '—';
  }
  const digits = String(value).trim();
  if (digits.length <= 4) {
    return '*'.repeat(digits.length);
  }
  return `${digits.slice(0, 3)}****${digits.slice(-4)}`;
};

const shouldMask = (house) => {
  if (props.canViewSensitiveInfo) {
    return false;
  }
  if (!house) {
    return true;
  }
  if (props.currentUser?.username && props.currentUser.username === house.sellerUsername) {
    return false;
  }
  if (isAdmin.value) {
    return false;
  }
  return true;
};

const sellerNameDisplay = (house) => {
  const name = house?.sellerName ?? '—';
  return shouldMask(house) ? maskName(name) : name;
};

const sellerUsernameDisplay = (house) => {
  const username = house?.sellerUsername ?? '—';
  return shouldMask(house) ? maskUsername(username) : username;
};

const contactNumberDisplay = (house) => {
  const contact = house?.contactNumber ?? '—';
  return shouldMask(house) ? maskPhone(contact) : contact;
};

const canEditHouse = (house) => {
  if (!canManage.value) {
    return false;
  }
  if (isAdmin.value) {
    return true;
  }
  return isSeller.value && props.currentUser?.username === house.sellerUsername;
};

const canDeleteHouse = (house) => canEditHouse(house);

const handleEdit = (house) => {
  if (!canEditHouse(house)) {
    return;
  }
  emit('edit', house);
};

const handleRemove = (house) => {
  if (!canDeleteHouse(house)) {
    return;
  }
  emit('remove', house);
};

const handleReview = (house, status) => {
  if (!isAdmin.value) {
    return;
  }
  emit('review', { houseId: house.id, status });
};

watch(
  () => props.houses,
  (list) => {
    const ids = new Set();
    (list ?? []).forEach((house) => {
      const id = house?.id;
      if (id == null) {
        return;
      }
      const key = String(id);
      ids.add(key);
      if (!selectedPayments[key]) {
        selectedPayments[key] = 'FULL';
      }
      if (selectedPayments[key] === 'INSTALLMENT' && !house.installmentMonthlyPayment) {
        selectedPayments[key] = 'FULL';
      }
    });
    Object.keys(selectedPayments).forEach((key) => {
      if (!ids.has(key)) {
        delete selectedPayments[key];
      }
    });
  },
  { immediate: true }
);

const resolvePaymentMethod = (house) => {
  if (!house?.id) {
    return 'FULL';
  }
  const key = String(house.id);
  const method = selectedPayments[key] ?? 'FULL';
  if (method === 'INSTALLMENT' && !house.installmentMonthlyPayment) {
    return 'FULL';
  }
  return method;
};

const handlePurchase = (house) => {
  if (!isBuyer.value || house.status !== 'APPROVED') {
    return;
  }
  emit('purchase', { house, paymentMethod: resolvePaymentMethod(house) });
};

const handleContactSeller = (house) => {
  if (!isBuyer.value || house.status !== 'APPROVED') {
    return;
  }
  emit('contact-seller', { sellerUsername: house.sellerUsername, house });
};

const statusLabel = (house) => listingStatusLabels.value[house?.status] ?? t('statuses.pending');

const statusClass = (house) => {
  switch (house?.status) {
    case 'APPROVED':
      return 'approved';
    case 'REJECTED':
      return 'rejected';
    case 'SOLD':
      return 'sold';
    default:
      return 'pending';
  }
};

const openDetail = (house) => {
  detailHouse.value = {
    ...house,
    imageUrls: Array.isArray(house.imageUrls) ? [...house.imageUrls] : []
  };
  detailCanViewSensitive.value = !shouldMask(house);
};

const closeDetail = () => {
  detailHouse.value = null;
  detailCanViewSensitive.value = false;
};
</script>

<style scoped>
.house-list {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  padding: 1.65rem;
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(var(--glass-blur));
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 0.85rem;
}

.list-header h2 {
  margin: 0;
  color: var(--color-text-strong);
}

.hint {
  color: var(--color-text-soft);
  font-size: 0.9rem;
}

.loading,
.empty {
  padding: 2.1rem 1.5rem;
  text-align: center;
  color: var(--color-text-muted);
  background: rgba(255, 255, 255, 0.85);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(226, 232, 240, 0.65);
}

.table-wrapper {
  overflow-x: auto;
  border-radius: calc(var(--radius-lg) - 0.35rem);
  border: 1px solid rgba(226, 232, 240, 0.5);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(calc(var(--glass-blur) / 3));
}

thead {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.85), rgba(37, 99, 235, 0.88));
  color: #fff;
}

th,
td {
  padding: 0.95rem 1.15rem;
  text-align: left;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
  vertical-align: top;
}

tbody tr:hover {
  background: rgba(241, 245, 249, 0.75);
}

.title-cell {
  min-width: 220px;
}

.title-button {
  border: none;
  background: transparent;
  padding: 0;
  margin: 0;
  font-size: 1rem;
  color: var(--color-text-strong);
  cursor: pointer;
  text-align: left;
}

.description {
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

.image-count {
  margin: 0.3rem 0 0;
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.price-cell {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.status {
  display: inline-flex;
  align-items: center;
  padding: 0.3rem 0.85rem;
  border-radius: var(--radius-pill);
  font-size: 0.78rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.status.pending {
  background: rgba(250, 204, 21, 0.18);
  color: #92400e;
}

.status.approved {
  background: rgba(34, 197, 94, 0.18);
  color: #166534;
}

.status.rejected {
  background: rgba(239, 68, 68, 0.2);
  color: #991b1b;
}

.status.sold {
  background: rgba(148, 163, 184, 0.2);
  color: var(--color-text-strong);
  letter-spacing: 0.04em;
}

:global(body[data-theme='dark']) :deep(.house-list .status.pending) {
  background: rgba(253, 224, 71, 0.16);
  color: #facc15;
}

:global(body[data-theme='dark']) :deep(.house-list .status.approved) {
  background: rgba(74, 222, 128, 0.16);
  color: #bbf7d0;
}

:global(body[data-theme='dark']) :deep(.house-list .status.rejected) {
  background: rgba(248, 113, 113, 0.18);
  color: #fecaca;
}

:global(body[data-theme='dark']) :deep(.house-list .status.sold) {
  background: rgba(148, 163, 184, 0.26);
  color: rgba(226, 232, 240, 0.92);
}

.status-tip {
  display: block;
  margin-top: 0.35rem;
  color: var(--color-text-soft);
  font-size: 0.82rem;
}

.muted {
  color: var(--color-text-soft);
  font-size: 0.9rem;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  min-width: 150px;
}

.payment-select {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.payment-select label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.payment-select select {
  padding: 0.4rem 0.6rem;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(148, 163, 184, 0.45);
  background: rgba(255, 255, 255, 0.92);
}

.btn {
  padding: 0.45rem 0.95rem;
  border-radius: var(--radius-pill);
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.btn.primary {
  background: var(--gradient-primary);
  color: #fff;
}

.btn.success {
  background: rgba(34, 197, 94, 0.18);
  color: #166534;
}

.btn.warning {
  background: rgba(250, 204, 21, 0.2);
  color: #92400e;
}

.btn.danger {
  background: rgba(239, 68, 68, 0.2);
  color: #991b1b;
}

.btn.ghost {
  background: rgba(226, 232, 240, 0.85);
  color: var(--color-text-strong);
  border: 1px solid rgba(148, 163, 184, 0.35);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

@media (max-width: 1024px) {
  table {
    font-size: 0.9rem;
  }

  th,
  td {
    padding: 0.75rem 0.85rem;
  }
}

@media (max-width: 768px) {
  .actions {
    min-width: auto;
  }
}
</style>
