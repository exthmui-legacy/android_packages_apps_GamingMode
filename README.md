# GamingMode

exTHmUI 游戏模式

## 性能调节器说明
当进入游戏模式或者用户调节 SeekBar 时, 游戏模式会设置 `sys.performance.level` 这个属性。\
设备维护者需要在代码中添加对这个属性变化的监听。
可以参考 [这个commit](https://github.com/exthmui-devices/android_device_xiaomi_raphael/commit/78ecd4bde0ee80e35cbfa21262e3399b59d44899)

### 属性值说明
- 0-6 : 数值越大性能越高
-  -1 : 恢复默认性能 

## 使用的第三方组件
- [EasyDanmaku](https://github.com/LittleFogCat/EasyDanmaku)
