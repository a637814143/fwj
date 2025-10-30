<template>
  <div class="ai-assistant">
    <header class="ai-assistant__header">
      <div>
        <h2>{{ t('aiAssistant.title') }}</h2>
        <p class="ai-assistant__subtitle">{{ t('aiAssistant.description') }}</p>
      </div>
      <div class="ai-assistant__header-actions">
        <span v-if="model" class="ai-assistant__model">{{ t('aiAssistant.modelLabel', { model }) }}</span>
        <button type="button" class="ghost" @click="refreshRelated" :disabled="loading">
          {{ t('aiAssistant.refresh') }}
        </button>
      </div>
    </header>

    <section class="ai-assistant__body">
      <aside class="ai-assistant__sidebar">
        <div class="ai-assistant__sidebar-header">
          <h3>{{ t('aiAssistant.relatedTitle') }}</h3>
          <p>{{ t('aiAssistant.relatedDescription') }}</p>
        </div>
        <ul class="ai-assistant__related">
          <li v-for="item in relatedQuestions" :key="item">
            <button type="button" class="link" :disabled="loading" @click="handleRelated(item)">
              {{ item }}
            </button>
          </li>
        </ul>
      </aside>

      <section class="ai-assistant__interaction">
        <form class="ai-assistant__form" @submit.prevent="submitQuestion">
          <label class="sr-only" for="ai-question">{{ t('aiAssistant.inputLabel') }}</label>
          <textarea
            id="ai-question"
            v-model="prompt"
            rows="4"
            :placeholder="t('aiAssistant.inputPlaceholder')"
            :disabled="loading"
          ></textarea>
          <div class="ai-assistant__form-actions">
            <span v-if="!hasKey" class="ai-assistant__hint">{{ t('aiAssistant.missingKey') }}</span>
            <button type="submit" class="primary" :disabled="!canSubmit">
              {{ loading ? t('aiAssistant.asking') : t('aiAssistant.ask') }}
            </button>
          </div>
        </form>

        <p v-if="error" class="ai-assistant__error">{{ error }}</p>

        <section class="ai-assistant__conversation">
          <header>
            <h3>{{ t('aiAssistant.conversationTitle') }}</h3>
          </header>
          <p v-if="exchanges.length === 0" class="ai-assistant__empty">{{ t('aiAssistant.emptyState') }}</p>
          <ul v-else>
            <li v-for="entry in exchanges" :key="entry.id" class="ai-assistant__exchange">
              <article>
                <h4>{{ t('aiAssistant.questionLabel') }}</h4>
                <p>{{ entry.question }}</p>
              </article>
              <article>
                <h4>{{ t('aiAssistant.answerLabel') }}</h4>
                <p v-if="entry.answer">{{ entry.answer }}</p>
                <p v-else class="ai-assistant__pending">{{ t('aiAssistant.pendingAnswer') }}</p>
              </article>
            </li>
          </ul>
        </section>
      </section>
    </section>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, default: 'http://localhost:8080/api' }
});

const translate = inject('translate', (key) => key);
const t = (key, vars) => translate(key, vars);

const prompt = ref('');
const error = ref('');
const loading = ref(false);
const exchanges = ref([]);
const relatedQuestions = ref([]);
const status = ref({ ready: false, model: '' });

const normalizedApiBaseUrl = computed(() => {
  const base = typeof props.apiBaseUrl === 'string' && props.apiBaseUrl.trim().length > 0
    ? props.apiBaseUrl.trim()
    : 'http://localhost:8080/api';
  return base.replace(/\/$/, '');
});

const hasKey = computed(() => Boolean(status.value?.ready));
const model = computed(() => status.value?.model ?? '');
const canSubmit = computed(() => hasKey.value && !loading.value && prompt.value.trim().length > 0);

const suggestionPool = [
  '购买二手房时如何核实房屋的产权信息？',
  '购入二手房需要重点检查哪些装修隐患？',
  '签订二手房买卖合同前需要注意哪些条款？',
  '如何评估二手房所在小区的未来保值潜力？',
  '申请二手房按揭贷款时银行关注哪些材料？',
  '二手房交易过程中公积金贷款有哪些限制？',
  '二手房过户时需要准备哪些税费？',
  '如何判断二手房的售价是否合理？',
  '二手房交易中常见的欺诈陷阱有哪些？',
  '二手房验房时应重点关注哪些结构问题？',
  '买二手房时如何查询房屋是否有抵押？',
  '在限购城市购买二手房需要满足哪些条件？'
];

const pickRelatedQuestions = () => {
  const pool = [...suggestionPool];
  const selections = [];
  while (pool.length > 0 && selections.length < 6) {
    const index = Math.floor(Math.random() * pool.length);
    selections.push(pool.splice(index, 1)[0]);
  }
  relatedQuestions.value = selections;
};

const refreshRelated = () => {
  pickRelatedQuestions();
};

const askAssistant = async (question) => {
  const trimmed = question.trim();
  if (!trimmed) {
    return;
  }
  const entry = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    question: trimmed,
    answer: ''
  };
  exchanges.value = [entry, ...exchanges.value];
  loading.value = true;
  error.value = '';
  try {
    const response = await fetch(`${normalizedApiBaseUrl.value}/assistant/ask`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        question: trimmed
      })
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      const message =
        payload?.message ||
        payload?.error ||
        t('aiAssistant.genericError');
      throw new Error(message);
    }
    const content = typeof payload?.answer === 'string' ? payload.answer : '';
    entry.answer = typeof content === 'string' && content.trim().length > 0
      ? content.trim()
      : t('aiAssistant.fallbackAnswer');
    if (typeof payload?.model === 'string') {
      status.value = {
        ready: hasKey.value,
        model: payload.model
      };
    }
    exchanges.value = [...exchanges.value];
  } catch (err) {
    entry.answer = '';
    exchanges.value = [...exchanges.value];
    const message = err instanceof Error && err.message ? err.message : t('aiAssistant.genericError');
    error.value = message;
  } finally {
    loading.value = false;
    prompt.value = '';
  }
};

const submitQuestion = async () => {
  if (!canSubmit.value) {
    if (!hasKey.value) {
      error.value = t('aiAssistant.missingKey');
    }
    return;
  }
  await askAssistant(prompt.value);
};

const handleRelated = async (question) => {
  if (loading.value) {
    return;
  }
  prompt.value = question;
  await submitQuestion();
};

const loadStatus = async () => {
  try {
    const response = await fetch(`${normalizedApiBaseUrl.value}/assistant/status`);
    if (!response.ok) {
      return;
    }
    const payload = await response.json().catch(() => null);
    status.value = {
      ready: Boolean(payload?.ready),
      model: typeof payload?.model === 'string' ? payload.model : ''
    };
  } catch (loadError) {
    console.warn('Failed to load AI assistant status', loadError);
    status.value = { ready: false, model: '' };
  }
};

onMounted(() => {
  pickRelatedQuestions();
  loadStatus();
});

watch(() => normalizedApiBaseUrl.value, () => {
  loadStatus();
});
</script>

<style scoped>
.ai-assistant {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  height: 100%;
}

.ai-assistant__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.ai-assistant__subtitle {
  color: var(--text-muted, #6b7280);
  margin-top: 0.25rem;
  font-size: 0.95rem;
  line-height: 1.4;
}

.ai-assistant__header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ai-assistant__model {
  font-size: 0.85rem;
  color: var(--text-muted, #6b7280);
}

.ai-assistant__body {
  display: grid;
  grid-template-columns: minmax(16rem, 22rem) 1fr;
  gap: 2rem;
  height: 100%;
}

.ai-assistant__sidebar {
  background: var(--surface-elevated, #ffffff);
  border: 1px solid var(--border-subtle, #e5e7eb);
  border-radius: 0.75rem;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ai-assistant__sidebar-header h3 {
  margin-bottom: 0.25rem;
}

.ai-assistant__sidebar-header p {
  font-size: 0.9rem;
  color: var(--text-muted, #6b7280);
}

.ai-assistant__related {
  display: grid;
  gap: 0.5rem;
  list-style: none;
  padding: 0;
  margin: 0;
}

.ai-assistant__related button {
  text-align: left;
  color: var(--primary, #2563eb);
  background: transparent;
  border: none;
  padding: 0;
  font-size: 0.95rem;
  cursor: pointer;
}

.ai-assistant__related button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-assistant__interaction {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.ai-assistant__form textarea {
  width: 100%;
  border: 1px solid var(--border-default, #d1d5db);
  border-radius: 0.75rem;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  line-height: 1.5;
  resize: vertical;
  min-height: 6rem;
}

.ai-assistant__form textarea:disabled {
  background: var(--surface-muted, #f9fafb);
}

.ai-assistant__form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.75rem;
}

.ai-assistant__hint {
  font-size: 0.85rem;
  color: #ef4444;
}

.ai-assistant__error {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  border-radius: 0.75rem;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(239, 68, 68, 0.25);
}

.ai-assistant__conversation ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ai-assistant__exchange {
  background: var(--surface-elevated, #ffffff);
  border: 1px solid var(--border-subtle, #e5e7eb);
  border-radius: 0.75rem;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ai-assistant__exchange article h4 {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-muted, #6b7280);
  margin-bottom: 0.25rem;
}

.ai-assistant__exchange article p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.5;
}

.ai-assistant__pending {
  color: var(--text-muted, #6b7280);
  font-style: italic;
}

.ai-assistant__empty {
  color: var(--text-muted, #6b7280);
}

@media (max-width: 1024px) {
  .ai-assistant__body {
    grid-template-columns: 1fr;
  }
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
