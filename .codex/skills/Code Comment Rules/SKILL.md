---
name: code-comment-rules
description: 代码注释规范，定义生成代码时的注释标准和 Lombok 使用规则
---

# Code Comment Rules

所有新生成或修改的代码必须遵循以下注释规范。

## 通用原则

- 注释应解释"为什么"，而不是简单重复代码做了什么。
- 避免无意义注释，例如：
  - 获取用户
  - 查询数据
  - 返回结果
- 保持注释与代码同步，修改代码时同步更新注释。
- 删除已经失效的注释。

---

## 类注释

所有 Controller、Service、Mapper、Configuration、Component 等公共类必须添加类注释。

示例：

```java
/**
 * 用户管理服务
 *
 * 负责用户信息维护、状态管理及权限相关业务。
 *
 * @author AI
 */
```

---

## 方法注释

所有 public 方法必须使用 JavaDoc。

必须包含：

- 方法作用
- 参数说明
- 返回值说明
- 重要业务说明（如有）

示例：

```java
/**
 * 根据用户ID查询用户信息。
 *
 * @param userId 用户ID
 * @return 用户信息
 */
```

不要生成：

```java
/**
 * 查询
 */
```

---

## 字段注释

Entity、DTO、VO 的字段必须添加说明。

例如：

```java
/**
 * 用户名称
 */
private String username;
```

---

## 复杂业务

满足以下情况必须添加行内注释：

- 复杂计算
- 特殊业务规则
- SQL优化
- 缓存逻辑
- 权限判断
- 状态流转
- 多线程
- 达梦数据库兼容处理

注释重点说明：

为什么这样做。

不要仅描述代码本身。

---

## 禁止事项

禁止生成：

// TODO

// 临时处理

// magic

// fix bug

// 这里不知道为什么

// 以后再改

除非用户明确要求。

---

## 输出要求

生成代码时：

- 注释应简洁准确。
- 不允许为了增加注释而增加注释。
- 不要生成模板化、重复、无意义的说明。

## Lombok 使用规范

为了保持代码简洁，项目统一使用 Lombok。

### Entity、DTO、VO,Domain

默认使用：

```java
@Getter
@Setter
```

禁止手动编写 getter() 和 setter() 方法。

示例：

```java
@Getter
@Setter
public class UserDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;
}
```

---

### 禁止事项

不要生成：

```java
public Long getId() {
    return id;
}

public void setId(Long id) {
    this.id = id;
}
```

除非用户明确要求不使用 Lombok。

---

### 其他 Lombok 注解

根据业务需要合理使用：

- `@NoArgsConstructor`
- `@AllArgsConstructor`
- `@Builder`
- `@EqualsAndHashCode`
- `@ToString`

不要为了省事直接使用 `@Data`。

优先使用：

```java
@Getter
@Setter
```

仅在确有需要时再添加其他 Lombok 注解，避免引入不必要的 `equals()`、`hashCode()`、`toString()` 等方法。