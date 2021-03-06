package com.example.pleasestop.vkonkurse;

import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import com.example.pleasestop.vkonkurse.Utils.Constans;
import com.example.pleasestop.vkonkurse.Utils.VkUtil;
import com.example.pleasestop.vkonkurse.model.Competition;
import com.example.pleasestop.vkonkurse.model.CompetitionsList;
import com.example.pleasestop.vkonkurse.model.IsMemberResult;
import com.example.pleasestop.vkonkurse.model.VkRequestTask;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


public class Repository {
    public static final String TAG ="jopa";
    public static final String TAG1="jopa1";
    public static final String TAG2="jopa2";
    @Inject
    ApiService apiService;
    @Inject
    SharedPreferences preferences;

    public String token;
    public String userID;
    public Integer vkDelay;
    public Integer contestRequestDelay;
    public Integer contestListDelay;

    ConcurrentLinkedQueue<VkRequestTask> vkRequestTasks;
    CopyOnWriteArraySet<String> vkRequestTaskIds;

    public Repository (){
        MyApp.getNetComponent().inject(this);
        vkRequestTasks = new ConcurrentLinkedQueue<>();
        vkRequestTaskIds = new CopyOnWriteArraySet<>();
    }

    public Observable<CompetitionsList<Competition>> loadAllCompetition(String vkUId) {
        return apiService.loadAllCompetition(vkUId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<CompetitionsList<Competition>> loadNotActiveCompetitions(String vkUId) {
        return apiService.loadNotActiveCompetitions(vkUId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<CompetitionsList<Competition>> loadAllCompetition(String vkUId, Integer delay) {
        return apiService.loadAllCompetition(vkUId)
                .subscribeOn(Schedulers.io())
                .delay(delay, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<CompetitionsList<Competition>> loadWithId(String vkUId, Integer id) {
        return apiService.loadCompetitionAfterId(vkUId, id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<IsMemberResult> loadResolution(Integer id, String vkUid, boolean isMember) {
        Log.i("groupcheck", "loadResolutian - " + id);
        return apiService.checkResolution(id, vkUid, isMember)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation());
    }

    public void participationDone(final Competition competition, String vkUid){
        apiService.participationDone(competition.getId().toString(), vkUid)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(new Consumer<JsonObject>() {
                    @Override
                    public void accept(JsonObject jsonObject) throws Exception {
                        competition.isClose = true;
                    }
                });
    }
    public Observable<JsonObject> getWall(final Competition competition) {
        Pair<String, String> pairIdAndPostid = VkUtil.getGroupAndPostIds(competition.getLink());
        String posts = "-" + pairIdAndPostid.first +
                "_" + pairIdAndPostid.second;
        String s1 = preferences.getString(Constans.TOKEN,"");
        String s2 = VKSdk.getApiVersion();
        return apiService.getWall(s1, s2, posts)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation());
    }

    public Observable<Competition> getTextFromPost(final  Competition competition, Integer delay){
        return getWall(competition)
                .delay(delay, TimeUnit.MILLISECONDS)
                .map(new Function<JsonObject, Competition>() {
            @Override
            public Competition apply(JsonObject jsonObject) throws Exception {
                if(jsonObject != null)
                    if(jsonObject.getAsJsonArray("response") != null) {
                        int size = jsonObject.getAsJsonArray("response").size();
                        if (size == 0) {
                            competition.setText("");
                            return competition;
                        }
                        if (jsonObject.getAsJsonArray("response").size() > 0) {
                            jsonObject = (JsonObject) jsonObject.getAsJsonArray("response").get(0);
                            String text = VkUtil.removeTextFromTwoSymbols('[', '|', jsonObject.get("text").getAsString());
                            text = text.replace("]", "");
                            competition.setText(text);
                            competition.setImageLinks(new ArrayList<String>());
                            competition.setListSponsorGroupId(VkUtil.getSponsorId(jsonObject.get("text").getAsString()));
                            try {
                                JsonArray jsonArray = jsonObject.getAsJsonArray("attachments");
                                for (JsonElement json : jsonArray) {
                                    competition.getImageLinks().add(((JsonObject) json).get("photo").getAsJsonObject().get("photo_807").getAsString());
                                }
                            } catch (Exception e) {

                            }
                        }
                    }
                return competition;
            }
        });

    }
    public void setLike(String ownerId, String itemId) {
        String str1 = preferences.getString(Constans.TOKEN,"");
        String str2 = VKSdk.getApiVersion();
        String str3 = "post";
        String str4 = "-" + ownerId;
        Integer str5 = Integer.valueOf(itemId);
        apiService.setLike(
                    str1,
                    str2,
                    str3,
                    str4,
                    str5
                )
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(new Consumer<JsonObject>() {
                    @Override
                    public void accept(JsonObject jsonObject) throws Exception {
                        Log.i(TAG1, jsonObject.toString());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.i(TAG1, throwable.toString());
                    }
                });
    }

    public void removeLike(String ownerId, String itemId) {
        String str1 = preferences.getString(Constans.TOKEN,"");
        String str2 = VKSdk.getApiVersion();
        String str3 = "post";
        String str4 = "-" + ownerId;
        Integer str5 = Integer.valueOf(itemId);
        apiService.removeLike(
                str1,
                str2,
                str3,
                str4,
                str5
        )
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(new Consumer<JsonObject>() {
                    @Override
                    public void accept(JsonObject jsonObject) throws Exception {
                        Log.i(TAG1, jsonObject.toString());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.i(TAG1, throwable.toString());
                    }
                });
    }

    public void joinToGroup(String groupId, Integer delay) {
        Log.i("groupcheck", groupId + "");
        if(VkUtil.idOrScreenNameOfGroup(groupId)){
            apiService.joinToGroup(
                    preferences.getString(Constans.TOKEN,""),
                    VKSdk.getApiVersion(),
                    groupId
            )
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .delay(delay, TimeUnit.MILLISECONDS)
                    .subscribe(new Consumer<JsonObject>() {
                        @Override
                        public void accept(JsonObject jsonObject) throws Exception {
                            Log.i("groupcheck", jsonObject.toString());
                            if(jsonObject.get("error") != null){
                                MyApp.fireBaseLog("capcha");
                            }
                            Log.i(TAG1, jsonObject.toString());
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.i(TAG1, throwable.toString());
                        }
                    });
        } else {
            apiService.getIdFromScreenName(preferences.getString(Constans.TOKEN,""),
                    VKSdk.getApiVersion(),
                    groupId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe(new Consumer<JsonObject>() {
                        @Override
                        public void accept(JsonObject jsonObject) throws Exception {
                            Log.i("groupcheck", " - idOrScreenNameOfGroup чеканье "  + jsonObject.toString());
                            if(jsonObject.get("error") == null) {
                                joinToGroup(String.valueOf(jsonObject.get("response").getAsJsonObject().get("object_id").getAsInt()), 1000);
                            } else {
                                joinToGroup(groupId, 4000);
                            }
                            Log.i(TAG1, jsonObject.toString());
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.i(TAG1, throwable.toString());
                        }
                    });
        }
    }

    public Observable<Integer> isMember(String groupId){
        return apiService.isMember(preferences.getString(Constans.TOKEN,""),
                VKSdk.getApiVersion(),
                groupId,
                userID,
                1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<JsonObject, Integer>() {
                    @Override
                    public Integer apply(JsonObject jsonObject) throws Exception {
                        if(jsonObject.get("error")!= null){
                            Log.i("groupcheck", "isMember" + groupId + jsonObject.toString());
                            return -100;
                        }
                        Log.i("groupcheck", "isMember" + groupId + jsonObject.toString());
                        return jsonObject.get("response").getAsJsonObject().get("member").getAsInt();
                    }
                });
    }



    public void joinToGroupSdk(final String groupId){
        Log.i(TAG, "test run");
        Single.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Log.i(TAG, "test run end");
                Log.i(TAG, "Происходит подписка на группу " + groupId);
                VKRequest request = new VKRequest("groups.join", VKParameters.from("access_token", token ,"group_id", groupId));
                final Integer[] resp = new Integer[1];
                resp[0] = 0;
                request.executeSyncWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);
                        Log.i(TAG, "Подписка оформлена на группу " + groupId);
                    }

                    @Override
                    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                        super.attemptFailed(request, attemptNumber, totalAttempts);
                    }

                    @Override
                    public void onError(VKError error) {
                        super.onError(error);
                        Log.i(TAG, "onError: ");
                    }

                });
                return resp[0];
            }
            })
            .delay(vkDelay, TimeUnit.SECONDS).subscribeOn(Schedulers.newThread())
            .observeOn(Schedulers.newThread())
            .subscribe();
    }

    public void leaveToGroupSdk(final String groupId){
        Log.i(TAG, "test run");
        Single.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Log.i(TAG, "test run end");
                Log.i(TAG, "Происходит подписка на группу " + groupId);
                VKRequest request = new VKRequest("groups.leave", VKParameters.from("access_token", token ,"group_id", groupId));
                final Integer[] resp = new Integer[1];
                resp[0] = 0;
                request.executeSyncWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);
                        Log.i(TAG, "ливнул с группы " + groupId);
                    }

                    @Override
                    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                        super.attemptFailed(request, attemptNumber, totalAttempts);
                    }

                    @Override
                    public void onError(VKError error) {
                        super.onError(error);
                        Log.i(TAG, "onError: ");
                    }

                });
                return resp[0];
            }
        })
                .delay(vkDelay, TimeUnit.SECONDS).subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe();
    }

    public void joinToSponsorGroup(final Competition competition, final boolean isVkSdk){
        getWall(competition).subscribe(new Consumer<JsonObject>() {
            @Override
            public void accept(JsonObject jsonObject) throws Exception {
                JsonArray jsonArray = jsonObject.getAsJsonArray("response");
                jsonObject = (JsonObject) jsonArray.get(0);
                String text = jsonObject.get("text").getAsString();

                competition.setListSponsorGroupId(VkUtil.getSponsorId(text));
                if(isVkSdk){
                    for(String id : competition.getListSponsorGroupId())
                        joinToGroupSdk(id);
                } else {
                    for(String id : competition.getListSponsorGroupId())
                        Observable.just(id).delay(1,TimeUnit.SECONDS).subscribe((v) -> joinToGroup(v,0));
                }
                Log.i(TAG1, jsonObject.toString());
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.i(TAG1, throwable.getMessage());
            }
        });
    }



    public void canselCompetition(Competition competition){
        String str1 = preferences.getString(Constans.TOKEN, "");
        String str2 = VKSdk.getApiVersion();
        String str3 = "post";
        String str4 = "-" + competition.getPairIdAndPostid().first;
        Integer str5 = Integer.valueOf(
                competition.getPairIdAndPostid().second);
        apiService.removeLike(
                str1,
                str2,
                str3,
                str4,
                str5
        )
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(new Consumer<JsonObject>() {
                    @Override
                    public void accept(JsonObject jsonObject) throws Exception {
                        Log.i(TAG1, jsonObject.toString());
                        for (String idGroup : competition.getListSponsorGroupId()) {
                            leaveToGroupSdk(idGroup);
                        }
                        competition.isClose = false;
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.i(TAG1, throwable.toString());
                    }
                });

    }


}
