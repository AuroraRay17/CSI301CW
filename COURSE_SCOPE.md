# CSI301 番茄钟：课件范围记录

资料来源：用户提供的 `/Users/wzw/Desktop/csi301/`，2026-11-14 核对。此文件用于约束后续番茄钟实现；具体作业要求仍需以课程正式文件为准。

## 已阅读的资料

| 资料 | 页/张 | 主要内容 |
|---|---:|---|
| 2 Android_UI_elements.pdf | 57 页 | Activity 生命周期、Intent 传值、View、XML 布局、ConstraintLayout、多屏幕适配 |
| 3 More_UI_Elements.pdf | 56 页 | LayoutInflater、事件监听、Spinner / AdapterView、UI 线程、Handler、Toast、通知及通知渠道 |
| 4 intent_and_Android_Components.pdf | 59 页 | Intent、Intent filter、Service、MediaPlayer 示例、BroadcastReceiver |
| 5 Permissions_Storage_and_Sensors.pdf | 56 页 | 权限、应用私有文件、SharedPreferences、SQLiteOpenHelper、SensorManager、位置服务 |
| 6 Android_Graphics.pdf | 56 页 | View / Canvas 绘图、invalidate、SurfaceView、Drawable |
| Touch_Events_and_Networking_Basics.pdf | 55 页 | 触摸事件、MotionEvent、Thread、HttpURLConnection、AsyncTask、Socket |
| Mobile_HCI_and_Context_Awareness.pdf | 44 页 | 移动端交互、隐私、情境感知及其限制 |
| How_to_learn_a_new_field_LMO.pdf | 31 页 | 学习新领域的方法；不是 Android API 教材 |
| W9.pptx | 194 张 | 算法复杂度、队列、优先队列；不是 Android API 教材 |

## 番茄钟可直接使用的课程技术

- **语言与界面**：Java、XML 布局、Activity、View / ViewGroup、按钮与文本、事件监听、ConstraintLayout / LinearLayout、Drawable。
- **导航与状态**：显式 Intent、Intent extras、Activity 生命周期回调；返回页面时重新读持久状态。
- **计时显示**：Handler / postDelayed 更新界面；计时依据保存的目标时间和暂停剩余量计算，不能仅靠每秒减一。
- **本地数据**：SharedPreferences 保存时长、声音开关和轻量计时状态；SQLiteOpenHelper 保存任务与专注记录。
- **提醒**：Toast、NotificationChannel、Notification.Builder、NotificationManager、PendingIntent；权限按课件的声明与运行时申请方式处理。
- **可选特色**：SensorManager / TYPE_LIGHT 环境提示；MediaPlayer 播放本地背景音，可按需使用 Service；Canvas / Drawable 绘制简单视觉效果。
- **交互原则**：移动屏幕适配、用户可控、传感器数据不可靠时降级、避免无意义权限。

## 暂不引入的实现

- 课件未讲解的 Jetpack Compose、Room、WorkManager、DataStore、依赖注入框架、云后端或第三方 AI SDK。
- 课件未讲解 AlarmManager 精确闹钟机制；不能仅凭课件中的 Handler/Thread 承诺锁屏或进程退出后准点通知。
- 不为了功能数量而引入网络、位置或传感器权限。

## 与产品草案的对应

首版按课程范围完成任务管理、专注/休息计时、暂停/恢复、自定义时长、本地记录、前台结束提示。后台提醒如果是正式作业硬性要求，需要先确认课程是否允许用课件外的 Android API，再决定实现。特色模块优先选择用户主动反馈形成简单、可解释的节奏建议；传感器和背景音可作为后续选项。

## 开发前需要核实

- 本目录没有正式的项目评分标准或最终作业说明。
- 还需确认最终使用的 Android Studio / Gradle 环境、测试设备和 Android 版本。
