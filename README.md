# jpromise

A java library providing a Promise-implementation very similar to that of ES6. 


```java
Deferred def = new Deferred();

Promise promise = def.then((result) -> {
  return result + 10;
});

promise.then((result) -> {
  System.out.println(result);
});

def.resolve(5);

```
