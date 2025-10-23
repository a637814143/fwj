<template>
  <div class="house-list">
    <div class="list-header">
      <div>
        <h2>房源列表</h2>
        <p>共 {{ houses.length }} 套房源</p>
      </div>
      <span v-if="!canManage" class="hint">切换至卖家或管理员账号以管理房源</span>
    </div>

    <div v-if="loading" class="loading">数据加载中...</div>
    <div v-else-if="houses.length === 0" class="empty">
      {{ canManage ? '暂未添加房源，请先通过右侧表单发布。' : '暂无可浏览的房源记录。' }}
    </div>

    <div v-else class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>房源信息</th>
            <th>定价方案</th>
            <th>状态</th>
            <th>卖家</th>
            <th>联系方式</th>
            <th>关键词</th>
            <th v-if="canViewCertificate">产权证明</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="house in houses" :key="house.id">
            <td>
              <strong>{{ house.title }}</strong>
              <p class="sub">地址：{{ house.address }}</p>
              <p class="description" v-if="house.description">{{ house.description }}</p>
              <p class="meta" v-if="house.listingDate">挂牌日期：{{ formatDate(house.listingDate) }}</p>
            </td>
            <td>
              <div class="pricing">
                <span>全款：￥{{ formatNumber(house.price) }} 万</span>
                <span v-if="house.installmentMonthlyPayment">
                  分期：￥{{ formatNumber(house.installmentMonthlyPayment) }} 万 × {{ house.installmentMonths }} 期
                </span>
              </div>
            </td>
            <td>
              <span :class="['status', statusClass(house)]">{{ statusLabel(house) }}</span>
              <p class="status-tip" v-if="house.reviewMessage">{{ house.reviewMessage }}</p>
            </td>
            <td>
              <div>{{ sellerUsernameDisplay(house) }}</div>
              <div class="muted">{{ sellerNameDisplay(house) }}</div>
            </td>
            <td>{{ contactNumberDisplay(house) }}</td>
            <td>
              <ul v-if="house.keywords && house.keywords.length" class="keywords">
                <li v-for="keyword in house.keywords" :key="`${house.id}-${keyword}`">{{ keyword }}</li>
              </ul>
              <span v-else class="muted">未设置</span>
            </td>
            <td v-if="canViewCertificate">
              <a
                v-if="house.propertyCertificateUrl"
                :href="house.propertyCertificateUrl"
                target="_blank"
                rel="noopener noreferrer"
              >
                查看证明
              </a>
              <span v-else class="muted">未上传</span>
            </td>
            <td class="actions">
              <div class="button-group" v-if="canManage">
                <button
                  v-if="canEditHouse(house)"
                  class="btn"
                  @click="handleEdit(house)"
                >
                  编辑
                </button>
                <button
                  v-if="canDeleteHouse(house)"
                  class="btn danger"
                  @click="handleRemove(house)"
                >
                  删除
                </button>
                <template v-if="isAdmin && house.status !== 'APPROVED'">
                  <button class="btn success" @click="handleReview(house, 'APPROVED')">审核通过</button>
                  <button class="btn warning" @click="handleReview(house, 'REJECTED')">驳回</button>
                </template>
              </div>
              <div v-else class="muted">无管理权限</div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, toRefs } from 'vue';

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

const listingStatusLabels = {
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
};

const sellerRoles = ['SELLER', 'LANDLORD'];
const isBuyer = computed(() => props.currentUser?.role === 'BUYER');
const isAdmin = computed(() => props.currentUser?.role === 'ADMIN');
const isSeller = computed(() => sellerRoles.includes(props.currentUser?.role));
const canViewCertificate = computed(() => isAdmin.value || isSeller.value);

const formatNumber = (value) => {
  if (value == null || value === '') {
    return '-';
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
};

const formatDate = (value) => {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleDateString('zh-CN');
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

const canDeleteHouse = (house) => {
  if (!canManage.value) {
    return false;
  }
  if (isAdmin.value) {
    return true;
  }
  return isSeller.value && props.currentUser?.username === house.sellerUsername;
};

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

const statusLabel = (house) => listingStatusLabels[house?.status] ?? '待审核';

const statusClass = (house) => {
  switch (house?.status) {
    case 'APPROVED':
      return 'approved';
    case 'REJECTED':
      return 'rejected';
    default:
      return 'pending';
  }
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

.sub {
  margin: 0.35rem 0 0;
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.description {
  margin: 0.35rem 0;
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

.meta {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.pricing {
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

.status.approved {
  background: rgba(34, 197, 94, 0.18);
  color: #166534;
}

.status.pending {
  background: rgba(250, 204, 21, 0.18);
  color: #92400e;
}

.status.rejected {
  background: rgba(239, 68, 68, 0.2);
  color: #b91c1c;
}

.status-tip {
  margin: 0.5rem 0 0;
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

.muted {
  color: rgba(148, 163, 184, 0.9);
  font-size: 0.85rem;
}

.keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.keywords li {
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-pill);
  background: rgba(224, 231, 255, 0.8);
  color: #4338ca;
  font-size: 0.82rem;
  font-weight: 600;
}

.actions {
  min-width: 220px;
}

.button-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.btn {
  padding: 0.5rem 1.05rem;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--gradient-primary);
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.22);
  transition: transform var(--transition-base), box-shadow var(--transition-base),
    background var(--transition-base);
}

.btn.danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  box-shadow: 0 16px 30px rgba(239, 68, 68, 0.22);
}

.btn.warning {
  background: linear-gradient(135deg, #f97316, #ea580c);
  box-shadow: 0 16px 30px rgba(249, 115, 22, 0.22);
}

.btn.success {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  box-shadow: 0 16px 30px rgba(34, 197, 94, 0.24);
}

.btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 24px 45px rgba(15, 23, 42, 0.2);
}

.btn:disabled {
  background: rgba(203, 213, 225, 0.85);
  color: rgba(71, 85, 105, 0.85);
  cursor: not-allowed;
  box-shadow: none;
}
</style>
