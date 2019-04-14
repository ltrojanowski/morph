# morph
A kotlin library for boilerplate free data transformation.

In any application written in a strongly typed language there inevitably arises the need to convert an object of one type into an object of a different type which shares a number of the same fields.
```
data class SearchEvent(query: String, lat: Float, lng: Float)
data class SearchAction(query: String, lat: Float, lng: Float)
val searchEvent = SearchEvent(...)
val searchAction = SearchAction( searchEvent.query, ...)
data class Book(title: String, isbn: String,rating: Int, authorId: Long)
data class Author(authorId: Long, name: String, birth: DateTime, death: DateTime?, shortBio: String)
date class BookWithAuthor(title: String, isbn: String,rating: Int, authorName: String)
fun bookWithAuthor(book: Book, author: Author): BookWithAuthor {
  BookWithAuthor(book.title, ...) // I'm too lazy to type this out
}
```
Code like this might not be complicated, but it accumulates throughout a code base and becomes unnecessarily burdensome to maintain.
Morph aims to remove these builder functions from your code using code generation. Annotated classes get an extension function which enables you to morph one data class into another like in the snipped below.
```
val event: SearchEvent = SearchEvent(...)
val action: SearchAction = event.into<SearchAction>{}.morph()
```
# Using morph
I found [jitpack](jitpack.io) to be the easies way to get morph.
To use morph in your gradle project simply add the following code in your root build.gradle at the end of repositories:
```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
then add the dependency
```
dependencies {
  ...
  implementation "com.github.ltrojanowski:morph:morph-api:-SNAPSHOT"
  kapt "com.github.ltrojanowski:morph:moprh-compiler:-SNAPSHOT"
}
```
If you would like to make a class morphable you need to annotate it with the `@Morph` annotation.

Below a sample usage

```
@Morph(from = [Boo::class, Baz::class])
data class Foo(val a: String, val b: Double, val c: Int, val d: Float, val e: List<String>)

// sources
dataclass Boo(val a: String, val b: Double, val c: Int, val d: Float, val e: List<String>)
data class Baz(val a: String, val b: Double, val c: Int, val d: Float)

fun main(args: Array<String>) {
  val boo = Boo("a", 1.0, 2, 3.0f, listOf("from boo"))
  val baz = Baz("a", 1.0, 2, 3.0f)
  val foo1 = boo.into<Foo>{}.morph()
  assert(foo1.a == boo.a)
  assert(foo1.b == boo.b)
  assert(foo1.c == boo.c)
  assert(foo1.d == boo.d)
  assert(foo1.e == boo.e)
  val foo2 = baz.into<Foo>{ e = listOf("inserted manually") }.morph()
  assert(foo2.a == boo.a)
  assert(foo2.b == boo.b)
  assert(foo2.c == boo.c)
  assert(foo2.d == boo.d)
  assert(foo2.e == listOf("inserted manually"))
}
```
