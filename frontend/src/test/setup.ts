import { config } from '@vue/test-utils'

// 预加载全部 mock 模块：组件通过动态 import('@/mock/modules/...') 加载 mock 数据，
// 预加载后这些动态 import 在测试中已落定，消除「冷启动动态 import」带来的等待时序 flaky。
import.meta.glob('../mock/modules/*.ts', { eager: true })

config.global.stubs = {
  RouterLink: {
    props: ['to'],
    template: `<a :href="typeof to === 'string' ? to : to.path"><slot /></a>`,
  },
}
