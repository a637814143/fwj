<template>
  <transition name="fade">
    <div v-if="visible" class="conversation-overlay" @click.self="emit('close')">
      <div class="conversation-panel">
        <header class="panel-header">
          <div>
            <h2>消息中心</h2>
            <p>和卖家实时沟通，确认细节与交易安排。</p>
          </div>
          <div class="header-actions">
            <button type="button" class="secondary" @click.stop="emit('refresh-conversations')" :disabled="loadingConversations">
              {{ loadingConversations ? '刷新中...' : '刷新列表' }}
            </button>
            <button type="button" class="ghost" @click.stop="emit('close')">关闭</button>
          </div>
        </header>
        <div class="panel-body">
          <aside class="conversation-list">
            <div class="list-header">
              <h3>对话</h3>
              <span class="count">{{ conversations.length }}</span>
            </div>
            <div v-if="loadingConversations" class="list-loading">正在加载对话...</div>
            <div v-else-if="conversations.length === 0" class="list-empty">尚未开启任何对话。</div>
            <ul v-else>
              <li
                v-for="conversation in conversations"
                :key="conversation.id"
                :class="{ active: conversation.id === activeConversationId }"
                @click="selectConversation(conversation.id)"
              >
                <strong>{{ otherParticipantName(conversation) }}</strong>
                <p class="preview" v-if="conversation.lastMessage">
                  <span class="sender">{{ conversation.lastMessage.senderUsername === currentUser?.username ? '我' : conversation.lastMessage.senderDisplayName }}：</span>
                  <span>{{ conversation.lastMessage.content }}</span>
                </p>
                <p class="preview muted" v-else>暂无消息</p>
              </li>
            </ul>
          </aside>
          <section class="message-area">
            <div v-if="error" class="alert">{{ error }}</div>
            <div v-if="!activeConversation" class="empty-state">
              <h3>选择一个对话开始沟通</h3>
              <p>在左侧点击卖家即可查看历史消息并发送新的内容。</p>
            </div>
            <template v-else>
              <header class="chat-header">
                <div>
                  <h3>{{ activeConversationTitle }}</h3>
                  <p>与卖家实时交流，确认看房、付款等细节。</p>
                </div>
                <button
                  type="button"
                  class="ghost"
                  @click="emit('select-conversation', null)"
                  v-if="messages.length === 0"
                >
                  返回列表
                </button>
              </header>
              <div class="messages" ref="messageListRef">
                <div v-if="loadingMessages" class="messages-loading">消息加载中...</div>
                <template v-else-if="messages.length > 0">
                  <article
                    v-for="message in messages"
                    :key="message.id"
                    :class="['message', message.senderUsername === currentUser?.username ? 'outgoing' : 'incoming']"
                  >
                    <header>
                      <span class="sender">{{ message.senderUsername === currentUser?.username ? '我' : message.senderDisplayName }}</span>
                      <time>{{ formatDateTime(message.createdAt) }}</time>
                    </header>
                    <p>{{ message.content }}</p>
                  </article>
                </template>
                <div v-else class="messages-empty">暂未发送任何消息，快来跟卖家聊一聊吧。</div>
              </div>
              <form class="composer" @submit.prevent="submitMessage">
                <textarea
                  v-model="draft"
                  placeholder="输入消息内容，最多 2000 字"
                  :maxlength="2000"
                  rows="3"
                ></textarea>
                <button type="submit" :disabled="!canSend" class="primary">
                  {{ sendingMessage ? '发送中...' : '发送' }}
                </button>
              </form>
            </template>
          </section>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  conversations: { type: Array, default: () => [] },
  activeConversationId: { type: [Number, null], default: null },
  messages: { type: Array, default: () => [] },
  loadingConversations: { type: Boolean, default: false },
  loadingMessages: { type: Boolean, default: false },
  sendingMessage: { type: Boolean, default: false },
  currentUser: { type: Object, default: null },
  error: { type: String, default: '' },
  prefill: { type: String, default: '' }
});

const emit = defineEmits([
  'close',
  'refresh-conversations',
  'select-conversation',
  'send-message',
  'prefill-consumed'
]);

const draft = ref('');
const messageListRef = ref(null);
const lastMessageCount = ref(0);

const activeConversation = computed(() =>
  props.conversations.find((item) => item.id === props.activeConversationId) ?? null
);

const activeConversationTitle = computed(() => {
  if (!activeConversation.value) {
    return '对话详情';
  }
  return otherParticipantName(activeConversation.value);
});

const canSend = computed(() => {
  if (!activeConversation.value || props.sendingMessage) {
    return false;
  }
  return draft.value.trim().length > 0 && draft.value.trim().length <= 2000;
});

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      draft.value = '';
      lastMessageCount.value = props.messages.length;
    } else {
      const normalized = typeof props.prefill === 'string' ? props.prefill.trim() : '';
      if (normalized) {
        draft.value = normalized;
        emit('prefill-consumed');
      }
    }
  }
);

watch(
  () => props.messages,
  async (messages) => {
    if (!Array.isArray(messages)) {
      return;
    }
    if (messages.length > lastMessageCount.value) {
      draft.value = '';
    }
    lastMessageCount.value = messages.length;
    await nextTick();
    const list = messageListRef.value;
    if (list) {
      list.scrollTop = list.scrollHeight;
    }
  },
  { deep: true }
);

watch(
  () => props.prefill,
  (value) => {
    if (!props.visible) {
      return;
    }
    const normalized = typeof value === 'string' ? value.trim() : '';
    if (normalized && normalized !== draft.value.trim()) {
      draft.value = normalized;
      emit('prefill-consumed');
    }
  }
);

const selectConversation = (conversationId) => {
  emit('select-conversation', conversationId);
};

const submitMessage = () => {
  if (!canSend.value || !activeConversation.value) {
    return;
  }
  emit('send-message', {
    conversationId: activeConversation.value.id,
    content: draft.value.trim()
  });
};

const otherParticipantName = (conversation) => {
  if (!conversation) {
    return '';
  }
  if (conversation.buyer?.username === props.currentUser?.username) {
    return conversation.seller?.displayName ?? conversation.seller?.username ?? '卖家';
  }
  return conversation.buyer?.displayName ?? conversation.buyer?.username ?? '买家';
};

const formatDateTime = (value) => {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return date.toLocaleString('zh-CN', { hour12: false });
};
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.conversation-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1.75rem;
  z-index: 50;
  backdrop-filter: blur(6px);
}

.conversation-panel {
  width: min(980px, 96vw);
  max-height: 92vh;
  background: var(--gradient-surface);
  border-radius: calc(var(--radius-lg) + 0.35rem);
  box-shadow: 0 40px 90px rgba(15, 23, 42, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1.1rem;
  padding: 1.8rem 2.2rem;
  background: linear-gradient(135deg, rgba(29, 78, 216, 0.95), rgba(37, 99, 235, 0.95));
  color: #fff;
}

.panel-header h2 {
  margin: 0;
}

.panel-header p {
  margin: 0.35rem 0 0;
  opacity: 0.9;
}

.header-actions {
  display: flex;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.panel-body {
  display: grid;
  grid-template-columns: 280px 1fr;
  min-height: 520px;
  flex: 1;
}

.conversation-list {
  border-right: 1px solid rgba(226, 232, 240, 0.6);
  padding: 1.6rem;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  background: rgba(248, 250, 252, 0.85);
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
}

.list-header h3 {
  margin: 0;
}

.count {
  background: rgba(224, 231, 255, 0.85);
  color: #1d4ed8;
  border-radius: var(--radius-pill);
  padding: 0.2rem 0.7rem;
  font-size: 0.85rem;
  font-weight: 600;
}

.list-loading,
.list-empty {
  padding: 1rem;
  background: rgba(241, 245, 249, 0.85);
  border-radius: var(--radius-md);
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

.conversation-list ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.conversation-list li {
  padding: 0.85rem 1.1rem;
  border-radius: var(--radius-lg);
  background: rgba(248, 250, 252, 0.9);
  cursor: pointer;
  transition: background var(--transition-base), transform var(--transition-base),
    box-shadow var(--transition-base);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.conversation-list li:hover {
  background: rgba(224, 231, 255, 0.95);
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.18);
}

.conversation-list li.active {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 18px 30px rgba(37, 99, 235, 0.25);
}

.conversation-list li.active .preview.muted {
  color: rgba(255, 255, 255, 0.8);
}

.preview {
  margin: 0.4rem 0 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preview .sender {
  font-weight: 600;
}

.preview.muted {
  color: rgba(148, 163, 184, 0.9);
}

.message-area {
  display: flex;
  flex-direction: column;
  padding: 1.6rem;
  gap: 1.1rem;
  background: rgba(255, 255, 255, 0.92);
}

.alert {
  background: rgba(254, 202, 202, 0.8);
  border-left: 4px solid #ef4444;
  color: #991b1b;
  padding: 0.8rem 1.05rem;
  border-radius: var(--radius-md);
}

.empty-state {
  margin: auto;
  text-align: center;
  color: var(--color-text-muted);
  max-width: 320px;
}

.empty-state h3 {
  margin-bottom: 0.6rem;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.chat-header h3 {
  margin: 0;
}

.chat-header p {
  margin: 0.35rem 0 0;
  color: var(--color-text-soft);
  font-size: 0.9rem;
}

.messages {
  flex: 1;
  overflow-y: auto;
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: var(--radius-lg);
  padding: 1.1rem;
  background: rgba(248, 250, 252, 0.9);
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.messages-loading,
.messages-empty {
  text-align: center;
  color: var(--color-text-soft);
  padding: 2rem 0;
}

.message {
  max-width: 78%;
  padding: 0.8rem 1.05rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.15);
  align-self: flex-start;
}

.message.outgoing {
  align-self: flex-end;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  box-shadow: 0 18px 30px rgba(37, 99, 235, 0.28);
}

.message header {
  display: flex;
  justify-content: space-between;
  font-size: 0.78rem;
  margin-bottom: 0.35rem;
  color: inherit;
}

.message.outgoing header {
  color: rgba(255, 255, 255, 0.85);
}

.message p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.composer {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.85rem;
  align-items: center;
}

.composer textarea {
  resize: vertical;
  min-height: 90px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(148, 163, 184, 0.32);
  padding: 0.8rem 1rem;
  font-size: 0.95rem;
  font-family: inherit;
  background: rgba(255, 255, 255, 0.9);
}

button.primary {
  background: var(--gradient-primary);
  border: none;
  color: #fff;
  padding: 0.8rem 1.6rem;
  border-radius: var(--radius-pill);
  cursor: pointer;
  font-weight: 600;
  box-shadow: 0 18px 35px rgba(37, 99, 235, 0.28);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

button.primary:disabled {
  background: rgba(147, 197, 253, 0.85);
  cursor: not-allowed;
  box-shadow: none;
}

button.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 26px 48px rgba(37, 99, 235, 0.32);
}

button.secondary {
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.55);
  color: #fff;
  border-radius: var(--radius-pill);
  padding: 0.55rem 1.25rem;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

button.secondary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

button.secondary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.22);
}

button.ghost {
  background: transparent;
  border: 1px solid rgba(148, 163, 184, 0.35);
  color: #1d4ed8;
  border-radius: var(--radius-pill);
  padding: 0.5rem 1.2rem;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

button.ghost:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.18);
}

@media (max-width: 900px) {
  .panel-body {
    grid-template-columns: 1fr;
  }

  .conversation-list {
    border-right: none;
    border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  }

  .message {
    max-width: 100%;
  }
}
</style>
