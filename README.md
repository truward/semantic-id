semantic-id
===========

# Overview

This library provides support for creating semantically meaningful identifiers for use in SOA environment.

Semantically meaningful identifiers (further referred simply as 'semantic IDs) allow encoded representation of internal IDs used within the service to be translated to and from opaque string IDs that conceal the nature of internal identifiers and thus allow flexibility of internal implementation (e.g. change from internal ID to external and vice versa).

Also semantic IDs allow easier debugging, i.e. when referred from system logs, developers can easily understand what service/entity particular ID refers to.

# Representation

Semantic ID includes 3 parts:

* Encoded service name and version, e.g. 'foo1'
* Optional, encoded entity name, e.g. 'user'
* Encoded ID, e.g. 'q6b60'

Examples of such IDs: ``foo1.user.q6b60``, ``req.9j35zf3``.

Requirements:

* ID should be case insensitive, i.e. semantic ID ``a.b.1cd`` should always be equivalent to ``A.B.1CD`` and vice versa.
* Encoded service name and version is always required part of semantic ID. E.g. ``foo1.qw1`` is a valid semantic ID (with service name ``foo`` and version ``1``), ``qw1`` is not.

# How to use

Add to dependencies in your ``pom.xml``

```xml
<dependency>
  <groupId>com.truward.semantic</groupId>
  <artifactId>semantic-id</artifactId>
  <version>1.0.0</version>
</dependency>
```

Then in java code:

```java
final IdCodec userIdCodec = SemanticIdCodec.forService("foo1").withEntityName("user");

//...

// create record:
final long id = database.insertNewRecord(userData);
return userIdCodec.encodeLong(id); // produces something like "foo1.user.1"


// get record
final long id = userIdCodec.decodeLong(semanticId /* e.g. "foo1.user.1" would be converted to 1, and converting of "bar.item.1" would not be possible (resulting in exception) */);
final User user = database.queryUser(id); // using internal ID
return mapToUser(user);
```

