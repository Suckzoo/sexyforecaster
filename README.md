# CS442 Project 1

- Title: `sexyforecaster`
- Author: Seokju Hong
- ID: 20164484

This report is also available on https://github.com/suckzoo/sexyforecaster/readme.md.

PDF version of this report may not support hyperlinks.

## How the app works

1. Get current latitude and longitude via [LocationManager](https://developer.android.com/reference/android/location/LocationManager.html)
2. With given GPS coordinate, scrap geographic information via
[wunderground](http://api.wunderground.com) API (endpoint: `geolookup/q/{latitude},{longitude}.json`)
3. Get state and city from the scraped geographic information (in JSON format)
4. With given city and state, get condition, forecast, hourly forecast via wunderground API.
  - condition endpoint: `condition/q/{state}/{city}.json`
  - forecast endpoint: `forecast/q/{state}/{city}.json`
  - hourly forecast endpoint: `hourly/q/{state}/{city}.json`
5. When we got data from each endpoint, bind data to view. This can be done in parallel manner.


## Implementation

### [Kotlin](https://kotlinlang.org)

`Kotlin` is a language which is statically typed, and runs over JVM, Android, and Web browsers.
With `Kotlin` language, we can write code safely with [null safe operators](https://kotlinlang.org/docs/reference/null-safety.html)
 in concise manner.

### UI Layout with [Anko](https://github.com/Kotlin/Anko)

`Anko` is a library which makes it possible to write layout in concise manner.
With `Anko`, we use DSL(Domain Specific Language) for layout, instead of boring XMLs.
Here is brief example of constructing `LinearLayout` vertically and put button on it.

```Kotlin
verticalLayout {
    val name = editText()
    button("Say Hello") {
        onClick { toast("Hello, ${name.text}!") }
    }
}
```

When scraping ends, I simply bound the data to the view like the following.
```Kotlin
mainUI.cityTextView.text = city
```
[code fragment](https://github.com/Suckzoo/sexyforecaster/blob/master/app/src/main/java/sexy/mycodeis/suckzoo/sexyforecaster/MainActivity.kt#L144)

[UI code](https://github.com/Suckzoo/sexyforecaster/blob/master/app/src/main/java/sexy/mycodeis/suckzoo/sexyforecaster/MainActivityUI.kt#L34-L38)

### Asynchronous HTTP communication with [Fuel](https://github.com/kittinunf/Fuel)

`Fuel` is a library which makes HTTP communication simple. Network communication is
an expensive I/O operation. As the main thread has responsibility for rendering components,
we should not waiting for the end of the HTTP communication with main thread
(i.e. blocking manner). Thankfully, `Fuel` makes it simple to communicate over HTTP
in asynchronous manner. The following is an example of scraping geographical information
via wunderground api.

```Kotlin
"/geolookup/q/$latitude,$longitude.json".httpGet().responseJson { request, response, result ->
    when (result) {
        is Result.Failure -> {
            //Failure handling
        }
        is Result.Success -> {
            val location = result.get().obj().getJSONObject("location")
            val state = location?.getString("state")
            val city = location?.getString("city")
            if (state != null && city != null) {
                // data binding
                mainUI.cityTextView.text = city
                // fetch weather condition and forecasts with geographic information
                // ...
            }
        }
    }
}
```
[code fragment](https://github.com/Suckzoo/sexyforecaster/blob/master/app/src/main/java/sexy/mycodeis/suckzoo/sexyforecaster/MainActivity.kt#L133-L154)

We register callback to `Fuel` module and `Fuel` handles the HTTP communication
with threads other than main thread. When communication ends, `Fuel` calls
the callback function we've registered and we can successfully fetch the data we've wanted.

`Fuel` library also supports `RxJava`, which makes it possible to write
HTTP communication in [reactive](http://reactivex.io/) manner. Unfortunately,
I couldn't spend much time to learn those styles. So I just wrote codes with
callback functions.
