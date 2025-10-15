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
                <button
                  type="button"
                  :disabled="user.username === currentUser?.username"
                  @click="toggle(user)"
                >
                  {{ user.blacklisted ? '解除黑名单' : '加入黑名单' }}
                </button>
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
  }
});

const emit = defineEmits(['toggle-blacklist', 'refresh']);

const roleLabels = {
  SELLER: '卖家',
  BUYER: '买家',
  ADMIN: '管理员'
};

const toggle = (user) => {
  emit('toggle-blacklist', { username: user.username, blacklisted: !user.blacklisted });
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
