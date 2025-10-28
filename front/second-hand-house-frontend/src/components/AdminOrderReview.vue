<template>
  <section class="admin-order-review">
    <header>
      <div>
        <h2>Escrow review</h2>
        <p>All order funds are held in escrow and released only after an administrator approves the payout.</p>
      </div>
      <button type="button" @click="$emit('refresh')">Refresh list</button>
    </header>

    <div v-if="loading" class="loading">Loading escrow orders…</div>
    <div v-else-if="!orders || orders.length === 0" class="empty">No escrow orders waiting for review.</div>
    <table v-else>
      <thead>
        <tr>
          <th>Order ID</th>
          <th>Listing</th>
          <th>Buyer</th>
          <th>Seller</th>
          <th>Escrow balance</th>
          <th>Payment method</th>
          <th>Created at</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="order in orders" :key="order.id">
          <td>{{ order.id }}</td>
          <td>
            <strong>{{ order.houseTitle }}</strong>
            <span class="muted">￥{{ formatAmount(order.amount) }}</span>
          </td>
          <td>{{ order.buyerDisplayName }} (@{{ order.buyerUsername }})</td>
          <td>{{ order.sellerDisplayName }} (@{{ order.sellerUsername }})</td>
          <td>
            <div>Escrow: ￥{{ formatAmount(order.adminHoldAmount || order.amount) }}</div>
            <div v-if="Number(order.platformFee) > 0" class="muted">Fee: ￥{{ formatAmount(order.platformFee) }}</div>
          </td>
          <td>{{ paymentMethodLabel(order.paymentMethod) }}</td>
          <td>{{ formatTime(order.createdAt) }}</td>
          <td class="actions">
            <button type="button" class="primary" @click="release(order, 'SELLER')">Release to seller</button>
            <button type="button" class="secondary" @click="release(order, 'BUYER')">Refund to buyer</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
const props = defineProps({
  orders: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['refresh', 'release']);

const formatAmount = (value) => {
  if (value == null) {
    return '0.00';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  return num.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const formatTime = (value) => {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString('en-US');
};

const paymentMethodLabel = (value) => {
  switch (value) {
    case 'INSTALLMENT':
      return 'Instalments';
    case 'FULL':
    default:
      return 'Full payment';
  }
};

const release = (order, decision) => {
  emit('release', { orderId: order.id, decision });
};
</script>

<style scoped>
.admin-order-review {
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  padding: 1.8rem;
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(var(--glass-blur));
}

header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

header h2 {
  margin: 0;
  color: var(--color-text-strong);
}

header p {
  margin: 0.3rem 0 0;
  color: var(--color-text-soft);
}

header button {
  align-self: center;
  padding: 0.45rem 1.1rem;
  border-radius: var(--radius-pill);
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.loading,
.empty {
  margin: 0;
  color: var(--color-text-soft);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
}

th {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

td strong {
  display: block;
  margin-bottom: 0.3rem;
  color: var(--color-text-strong);
}

.muted {
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.actions {
  display: flex;
  gap: 0.45rem;
}

.actions button {
  padding: 0.4rem 0.85rem;
  border-radius: var(--radius-pill);
  border: none;
  font-weight: 600;
  cursor: pointer;
}

.actions .primary {
  background: rgba(34, 197, 94, 0.2);
  color: #15803d;
}

.actions .secondary {
  background: rgba(248, 113, 113, 0.18);
  color: #b91c1c;
}

.actions button:hover {
  opacity: 0.85;
}
</style>
