package eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models

import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

annotation class ChildKey(val name: String)

interface ApiInterface {
    @GET("profile")
    fun getProfile(): Call<Profile>

    @ChildKey("id")
    @GET("children")
    fun getChildEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<Child>>

    @ChildKey("child")
    @GET("sleep")
    fun getSleepEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<SleepEntry>>

    @ChildKey("child")
    @GET("feedings")
    fun getFeedingEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<FeedingEntry>>

    @ChildKey("child")
    @GET("tummy-times")
    fun getTummyTimeEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<TummyTimeEntry>>

    @ChildKey("child")
    @GET("pumping")
    fun getPumpingEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<PumpingEntry>>

    @ChildKey("child")
    @GET("changes")
    fun getChangeEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<ChangeEntry>>

    @ChildKey("child")
    @GET("notes")
    fun getNoteEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<NoteEntry>>

    @ChildKey("child")
    @GET("temperature")
    fun getTemperatureEnties(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<TemperatureEntry>>

    @ChildKey("child")
    @GET("weight")
    fun getWeightEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<WeightEntry>>

    @ChildKey("child")
    @GET("height")
    fun getHeightEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<HeightEntry>>

    @ChildKey("child")
    @GET("head-circumference")
    fun getHeadCircumferenceEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @QueryMap extraArgs: Map<String, String>,
    ): Call<PaginatedEntries<HeadCircumferenceEntry>>

    @DELETE("{type}/{id}/")
    fun genericDeleteEntry(
        @Path(value = "type", encoded = true) apiPath: String,
        @Path(value = "id", encoded = true) id: Int,
    ): Call<Any>
}
