<template>
  <section class="admin-order-review">
    <header>
      <div>
        <h2>交易审核</h2>
        <p>所有订单款项均由系统托管，完成交付后由管理员确认发放。</p>
      </div>
      <button type="button" @click="$emit('refresh')">刷新列表</button>
    </header>

    <div v-if="loading" class="loading">待审核订单加载中...</div>
    <div v-else-if="!orders || orders.length === 0" class="empty">暂无待审核的托管订单。</div>
    <table v-else>
      <thead>
        <tr>
          <th>订单编号</th>
          <th>房源</th>
          <th>买家</th>
          <th>卖家</th>
          <th>托管金额</th>
          <th>支付方式</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="order in orders" :key="order.id">
          <td>{{ order.id }}</td>
          <td>
            <strong>{{ order.houseTitle }}</strong>
            <span class="muted">￥{{ formatAmount(order.amount) }}</span>
          </td>
          <td>{{ order.buyerDisplayName }}（@{{ order.buyerUsername }}）</td>
          <td>{{ order.sellerDisplayName }}（@{{ order.sellerUsername }}）</td>
          <td>
            <div>托管：￥{{ formatAmount(order.adminHoldAmount || order.amount) }}</div>
            <div v-if="Number(order.platformFee) > 0" class="muted">抽成：￥{{ formatAmount(order.platformFee) }}</div>
          </td>
          <td>{{ paymentMethodLabel(order.paymentMethod) }}</td>
          <td>{{ formatTime(order.createdAt) }}</td>
          <td class="actions">
            <button type="button" class="primary" @click="release(order, 'SELLER')">发放给卖家</button>
            <button type="button" class="secondary" @click="release(order, 'BUYER')">退回买家</button>
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
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const formatTime = (value) => {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString('zh-CN');
};

const paymentMethodLabel = (value) => {
  switch (value) {
    case 'INSTALLMENT':
      return '分期付款';
    case 'FULL':
    default:
      return '全款支付';
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
