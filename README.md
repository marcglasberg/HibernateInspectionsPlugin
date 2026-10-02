# Hibernate Inspections Plugin for IntelliJ IDEA

Finds Hibernate bugs that fail silently, and shows them as warnings in your code.

Plugin page: https://plugins.jetbrains.com/plugin/7866-hibernate-inspections

## How to install

In IntelliJ IDEA, go to **Settings > Plugins > Marketplace**, search for **Hibernate Inspections**,
and click **Install**.

The current version needs IntelliJ IDEA 2026.2 or newer.

## Why use it?

In some cases Hibernate doesn't work correctly, but doesn't show any error either.
For example, a field may be `null` when it shouldn't be, or some data may not be saved.
Bugs like these are hard to find.

This plugin finds some of these cases while you type, and tells you how to fix them.

You can see and configure its checks (called "inspections") in
**Settings > Editor > Inspections > Hibernate inspections**:

| Inspection | What it finds |
|---|---|
| [Persisted class is final](#persisted-class-is-final) | An entity class declared as `final` |
| [Final method of a persisted class uses direct field access](#final-method-of-a-persisted-class-uses-direct-field-access) | A `final` method that reads or writes a field of an entity |
| [Embeddable subclasses embeddable](#embeddable-subclasses-embeddable) | An `@Embeddable` class that extends another `@Embeddable` class (only a problem before Hibernate 6.6) |

It works with both kinds of annotations:

* `jakarta.persistence` (Hibernate 6 and newer)
* `javax.persistence` (Hibernate 5 and older)

The plugin only checks classes that use annotations (`@Entity`, `@MappedSuperclass` or `@Embeddable`).
It does not check XML mappings.

---

## Persisted class is final

**What it finds:** a class marked with `@Entity`, `@MappedSuperclass` or `@Embeddable` that is also `final`.

```java
@Entity
public final class Cat { ... }   // Warning
```

**Why it's a problem:** to load data only when it's needed ("lazy loading"), Hibernate creates
a subclass of your class, called a *proxy*. It can't create a subclass of a `final` class.
Your code still works, but Hibernate can't load this class lazily, which can make your app slower.

From the Hibernate documentation:

> A central feature of Hibernate, proxies (lazy loading), depends upon the persistent class being either non-final,
> or the implementation of an interface that declares all public methods.

**How to fix it:**

* Remove `final` from the class. The plugin can do this for you: press **Alt+Enter** on the warning,
  and choose **Remove 'final' modifier**.
* Or, if you really want the class to be `final`, turn off proxies for it with `@Proxy(lazy=false)`.
  Note: `@Proxy` is deprecated since Hibernate 6.2, and was removed in Hibernate 7.

Records are not reported. They are always `final`, but they can't be entities, and they're fine as embeddables.

**More information:**

* [Hibernate documentation](https://docs.jboss.org/hibernate/orm/5.0/manual/en-US/html/ch04.html#persistent-classes-pojo-final-example-disable-proxies-ann)
* [Stack Overflow question](https://stackoverflow.com/questions/6608222/does-a-final-method-prevent-hibernate-from-creating-a-proxy-for-such-an-entity)

---

## Final method of a persisted class uses direct field access

**What it finds:** a `final` method, in a class marked with `@Entity`, `@MappedSuperclass` or `@Embeddable`,
that uses one of the class's fields directly.

```java
@Entity
public class Cat {
    private String name;

    public final String getName() {   // Warning
        return name;
    }
}
```

**Why it's a problem:** this is almost always a bug, and it fails silently.
The proxy that Hibernate creates (see above) is a subclass of your class, and its fields are always empty.
The proxy can't override a `final` method, so when you call that method on a proxy, it reads the proxy's
empty fields. You get `null`, `0` or `false` instead of the real value, and no error.

A `final` method is fine if it doesn't use fields directly. For example, it may call `getName()` instead
of reading `name`. Static methods and static fields are not reported, because proxies don't affect them.

**How to fix it:**

* Remove `final` from the method (**Alt+Enter**, then **Remove 'final' modifier**).
* Or, don't use fields directly in `final` methods. Call other (non-final) methods instead.

**When it's OK to ignore the warning:** a `final` `getId()` can be useful. If you use field access,
calling a normal `getId()` on a proxy makes Hibernate load the whole object from the database, only to
return the id. A `final` `getId()` can get the id from the proxy instead, without loading anything:

```java
@SuppressWarnings("AccessingFieldFromAFinalMethodOfPersistedClass")
public final long getId() {
    if (this instanceof HibernateProxy) {
        return (long) ((HibernateProxy) this).getHibernateLazyInitializer().getIdentifier();
    } else {
        return id;
    }
}
```

The `@SuppressWarnings` line turns off the warning for this method only.

**More information:**

* [IntelliJ IDEA issue](https://youtrack.jetbrains.com/issue/IDEA-128132)
* [Stack Overflow question](https://stackoverflow.com/questions/6608222/does-a-final-method-prevent-hibernate-from-creating-a-proxy-for-such-an-entity)

---

## Embeddable subclasses embeddable

**What it finds:** an `@Embeddable` class that extends another `@Embeddable` class.

```java
@Embeddable
public class Address { ... }

@Embeddable
public class HomeAddress extends Address { ... }   // Warning
```

**Why it's a problem:** before version 6.6, Hibernate doesn't support this. It saves only the fields
declared in the exact class used in the entity, not the fields of its parent class.
Also, when loading the entity, Hibernate may silently set the whole embedded object to `null`.

**Hibernate 6.6 and newer support this**
([announcement](https://in.relation.to/2024/07/12/embeddable-inheritance/)).
If your project uses Hibernate 6.6 or newer, the plugin detects it and doesn't show this warning.

**How to fix it (before Hibernate 6.6):** don't extend one `@Embeddable` class from another.
For example, copy the fields into the subclass, or upgrade Hibernate.

**More information:**

* Hibernate issues: [HHH-1152](https://hibernate.atlassian.net/browse/HHH-1152),
  [HHH-1910](https://hibernate.atlassian.net/browse/HHH-1910),
  [HHH-3455](https://hibernate.atlassian.net/browse/HHH-3455)
* Stack Overflow questions:
  [1](https://stackoverflow.com/questions/29278249/hibernate-embeddable-class-which-extends-another-embeddable-class-properties),
  [2](https://stackoverflow.com/questions/917974/hibernate-embeddable-inheritance),
  [3](https://stackoverflow.com/questions/29788716/jpa-2-0-embedded-inherited-abstract-class)

---

## Contributing

Found a bug, or have an idea for a new inspection?
[Open an issue](https://github.com/marcglasberg/HibernateInspectionsPlugin/issues) or send a pull request.

To build the plugin, run its tests, or publish a new version, see [UPDATED.md](UPDATED.md).

## Author

Created by **Marcelo Glasberg**:
[glasberg.dev](https://glasberg.dev) ·
[GitHub](https://github.com/marcglasberg) ·
[LinkedIn](https://www.linkedin.com/in/marcglasberg/) ·
[Stack Overflow](https://stackoverflow.com/users/3411681/marcg)
