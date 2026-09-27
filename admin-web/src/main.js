import { createApp } from 'vue'
import App from './App.vue'
import router from './router/index.js'
import { ElButton, ElInput, ElSelect, ElOption, ElPagination, ElRadioGroup, ElRadioButton } from 'element-plus'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/pagination/style/css'
import 'element-plus/es/components/radio-group/style/css'
import 'element-plus/es/components/radio-button/style/css'
import './assets/component-theme.css'

const app = createApp(App)
for (const component of [ElButton, ElInput, ElSelect, ElOption, ElPagination, ElRadioGroup, ElRadioButton]) {
  app.component(component.name, component)
}
app.use(router).mount('#app')
