<template>
  <section class="admin-board">
    <header>
      <div>
        <h2>信誉评分面板</h2>
        <p>系统根据交易行为自动评估买家与卖家信誉，支持管理员拉黑或解除黑名单。</p>
      </div>
      <button type="button" @click="$emit('refresh')">刷新数据</button>
    </header>

    <div v-if="loading" class="loading">信誉数据加载中...</div>
    <div v-else class="content">
      <section class="pending" aria-label="待审核房源列表">
        <header class="pending-header">
          <div>
            <h3>房源审核任务</h3>
            <p>审核卖家提交的房源资料，确保重复上架与异常信息被拦截。</p>
          </div>
          <span class="badge" :class="{ empty: pendingHouses.length === 0 }">
            待处理 {{ pendingHouses.length }} 套
          </span>
        </header>
        <div v-if="pendingHouses.length === 0" class="empty-card">暂无待审核房源，已全部处理完毕。</div>
        <table v-else class="pending-table">
          <thead>
            <tr>
              <th>房源</th>
              <th>卖家</th>
              <th>提交时间</th>
              <th>产权证明</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="house in pendingHouses" :key="house.id">
              <td>
                <strong>{{ house.title }}</strong>
                <p class="sub">地址：{{ house.address }}</p>
                <p class="meta">关键词：{{ formatKeywords(house.keywords) }}</p>
              </td>
              <td>
                <div>{{ house.sellerName ?? '—' }}</div>
                <div class="muted">@{{ house.sellerUsername }}</div>
              </td>
              <td>{{ formatDate(house.createdAt ?? house.listingDate) }}</td>
              <td>
                <a
                  v-if="house.propertyCertificateUrl"
                  :href="house.propertyCertificateUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  查看材料
                </a>
                <span v-else class="muted">未上传</span>
              </td>
              <td class="actions">
                <button class="btn success" type="button" @click="approve(house)">通过</button>
                <button class="btn warning" type="button" @click="reject(house)">驳回</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="summary" v-if="overview">
        <div>
          <span class="label">黑名单账号</span>
          <strong>{{ overview.blacklistedCount }}</strong>
        </div>
        <div>
          <span class="label">卖家数量</span>
          <strong>{{ overview.sellers.length }}</strong>
        </div>
        <div>
          <span class="label">买家数量</span>
          <strong>{{ overview.buyers.length }}</strong>
        </div>
      </section>

      <section class="leaderboard">
        <div>
          <h3>卖家信誉榜</h3>
          <table>
            <thead>
              <tr>
                <th>卖家</th>
                <th>信誉分</th>
                <th>违约次数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="seller in overview?.sellers ?? []" :key="`seller-${seller.username}`">
                <td>
                  <strong>{{ seller.displayName }}</strong>
                  <span class="username">@{{ seller.username }}</span>
                </td>
                <td>{{ seller.reputationScore }}</td>
                <td>{{ seller.reservationBreaches }}</td>
              </tr>
              <tr v-if="!overview || overview.sellers.length === 0">
                <td colspan="3" class="empty">暂无卖家数据</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div>
          <h3>买家信誉榜</h3>
          <table>
            <thead>
              <tr>
                <th>买家</th>
                <th>信誉分</th>
                <th>退回次数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="buyer in overview?.buyers ?? []" :key="`buyer-${buyer.username}`">
                <td>
                  <strong>{{ buyer.displayName }}</strong>
                  <span class="username">@{{ buyer.username }}</span>
                </td>
                <td>{{ buyer.reputationScore }}</td>
                <td>{{ buyer.returnCount }}</td>
              </tr>
              <tr v-if="!overview || overview.buyers.length === 0">
                <td colspan="3" class="empty">暂无买家数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="user-table">
        <h3>账号黑名单管理</h3>
        <table>
          <thead>
            <tr>
              <th>账号</th>
              <th>角色</th>
              <th>信誉分</th>
              <th>违约/退回</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.username" :class="{ blacklisted: user.blacklisted }">
              <td>
                <strong>{{ user.displayName }}</strong>
                <span class="username">@{{ user.username }}</span>
              </td>
              <td>{{ roleLabels[user.role] ?? user.role }}</td>
              <td>{{ user.reputationScore }}</td>
              <td>
                <span v-if="user.role === 'SELLER'">违约 {{ user.reservationBreaches }}</span>
                <span v-else>退回 {{ user.returnCount }}</span>
              </td>
              <td>
                <span class="status" :class="user.blacklisted ? 'bad' : 'good'">
                  {{ user.blacklisted ? '已拉黑' : '正常' }}
                </span>
              </td>
              <td>
                <div class="actions">
                  <button
                    type="button"
                    :disabled="user.username === currentUser?.username"
                    @click="toggle(user)"
                  >
                    {{ user.blacklisted ? '解除黑名单' : '加入黑名单' }}
                  </button>
                  <button
                    type="button"
                    class="danger"
                    :disabled="user.role === 'ADMIN' || user.username === currentUser?.username"
                    @click="remove(user)"
                  >
                    删除账号
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!users || users.length === 0">
              <td colspan="6" class="empty">暂无账号数据。</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </section>
</template>

<script setup>
const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  overview: {
    type: Object,
    default: null
  },
  users: {
    type: Array,
    default: () => []
  },
  currentUser: {
    type: Object,
    default: null
  },
  pendingHouses: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['toggle-blacklist', 'refresh', 'delete-user', 'review-house']);

const roleLabels = {
  SELLER: '卖家',
  BUYER: '买家',
  ADMIN: '管理员'
};

const toggle = (user) => {
  emit('toggle-blacklist', { username: user.username, blacklisted: !user.blacklisted });
};

const remove = (user) => {
  emit('delete-user', { username: user.username });
};

const approve = (house) => {
  emit('review-house', { houseId: house.id, status: 'APPROVED' });
};

const reject = (house) => {
  emit('review-house', { houseId: house.id, status: 'REJECTED' });
};

const formatDate = (value) => {
  if (!value) {
    return '—';
  }
  try {
    return new Date(value).toLocaleString('zh-CN');
  } catch (error) {
    return value;
  }
};

const formatKeywords = (keywords = []) => {
  if (!Array.isArray(keywords) || keywords.length === 0) {
    return '未设置';
  }
  return keywords.join('、');
};
</script>

<style scoped>
.admin-board {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.1);
  padding: 1.5rem;
}

header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: center;
}

header h2 {
  margin: 0;
  color: #1e293b;
}

header p {
  margin: 0.35rem 0 0;
  color: #475569;
  max-width: 36rem;
}

header button {
  border: none;
  border-radius: 999px;
  background: #2563eb;
  color: #fff;
  padding: 0.55rem 1.5rem;
  cursor: pointer;
  font-weight: 600;
}

.pending {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.pending-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.pending-header h3 {
  margin: 0;
  color: #1e293b;
}

.pending-header p {
  margin: 0.35rem 0 0;
  color: #475569;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  background: #2563eb;
  color: #fff;
  font-weight: 600;
}

.badge.empty {
  background: #94a3b8;
}

.empty-card {
  padding: 1rem;
  border-radius: 0.85rem;
  background: #f8fafc;
  color: #64748b;
  text-align: center;
}

.pending-table {
  width: 100%;
  border-collapse: collapse;
  background: #f8fafc;
  border-radius: 0.85rem;
  overflow: hidden;
}

.pending-table th,
.pending-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  font-size: 0.9rem;
}

.pending-table th {
  background: #dbeafe;
  color: #1d4ed8;
}

.pending-table .sub,
.pending-table .meta,
.pending-table .muted {
  margin: 0.25rem 0 0;
  color: #64748b;
  font-size: 0.85rem;
}

.pending-table .meta {
  font-style: italic;
}

.pending-table .muted {
  font-size: 0.8rem;
}

.btn {
  border: none;
  border-radius: 999px;
  padding: 0.45rem 1.1rem;
  cursor: pointer;
  font-weight: 600;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.btn.success {
  background: #16a34a;
  color: #fff;
}

.btn.warning {
  background: #f59e0b;
  color: #1f2937;
}

.btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.15);
}

.loading {
  text-align: center;
  padding: 1.5rem;
  color: #475569;
}

.summary {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.summary > div {
  background: rgba(37, 99, 235, 0.08);
  border-radius: 0.9rem;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.summary .label {
  font-size: 0.85rem;
  color: #1d4ed8;
}

.summary strong {
  font-size: 1.4rem;
  color: #1e293b;
}

.actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.actions .danger {
  background: #dc2626;
  color: #fff;
}

.actions .danger:disabled {
  background: #fca5a5;
  cursor: not-allowed;
}

.leaderboard {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.leaderboard table,
.user-table table {
  width: 100%;
  border-collapse: collapse;
  background: #f8fafc;
  border-radius: 0.85rem;
  overflow: hidden;
}

.leaderboard th,
.leaderboard td,
.user-table th,
.user-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  font-size: 0.9rem;
}

.leaderboard th,
.user-table th {
  background: #e0e7ff;
  color: #1e3a8a;
}

.username {
  display: block;
  font-size: 0.75rem;
  color: #64748b;
}

.empty {
  text-align: center;
  color: #94a3b8;
  font-size: 0.9rem;
}

.user-table {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.user-table h3 {
  margin: 0;
  color: #1e293b;
}

.muted {
  color: #64748b;
}

.user-table button {
  border: none;
  border-radius: 0.65rem;
  padding: 0.4rem 0.9rem;
  cursor: pointer;
  font-weight: 600;
  background: #2563eb;
  color: #fff;
}

.user-table button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.user-table tr.blacklisted {
  background: rgba(239, 68, 68, 0.08);
}

.status {
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  font-weight: 600;
  font-size: 0.85rem;
}

.status.good {
  background: rgba(34, 197, 94, 0.15);
  color: #16a34a;
}

.status.bad {
  background: rgba(239, 68, 68, 0.15);
  color: #dc2626;
}
</style>
