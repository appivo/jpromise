# jpromise

A java library providing a Promise-implementation very similar to that of ES6. 


```java
Deferred def = new Deferred();

def.resolve(5);

def.then((result) -> {
  return result + 10;
});
```
