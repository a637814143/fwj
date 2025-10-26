import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import ManageView from '../views/ManageView.vue';
import ReviewView from '../views/ReviewView.vue';
import OrdersView from '../views/OrdersView.vue';
import VerifyView from '../views/VerifyView.vue';
import AdminView from '../views/AdminView.vue';
import ReputationView from '../views/ReputationView.vue';

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/verify', name: 'verify', component: VerifyView },
  { path: '/manage', name: 'manage', component: ManageView },
  { path: '/orders', name: 'orders', component: OrdersView },
  { path: '/review', name: 'review', component: ReviewView },
  { path: '/admin', name: 'admin', component: AdminView },
  { path: '/reputation', name: 'reputation', component: ReputationView }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
