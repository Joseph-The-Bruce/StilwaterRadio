package com.example.saintsrowradio;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.CommandButton;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionResult;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MediaPlayerService extends MediaLibraryService {

    private ExoPlayer player;
    private MediaLibrarySession mediaLibrarySession;

    List<String> commercials = new ArrayList<>();
    List<String> news = new ArrayList<>();
    List<String> callers = new ArrayList<>();
    List<String> intros = new ArrayList<>();
    List<String> newsIntros = new ArrayList<>();
    List<String> outros = new ArrayList<>();
    List<String> themes = new ArrayList<>();
    List<String> songs = new ArrayList<>();
    List<String> songQueue = new ArrayList<>();
    List<String> introList = new ArrayList<>();
    List<String> outroList = new ArrayList<>();
    String soundType;
    String songName;
    String currentSongFile;
    String currentStationName = "Stilwater Radio";
    String currentStationId = "";
    int newsItr = 0;
    int commItr = 0;
    private final Random random = new Random();
    
    // Configurable rotation settings
    int commercialsPerSong = 3;
    int songsBeforeNews = 5;
    boolean includeSingAlongs = false;
    boolean disableMenuMusic = false;
    
    // Saints Radio inclusions
    boolean includeKrunch = true;
    boolean includeKrhyme = true;
    boolean includeMix = true;
    boolean includeGenx = true;
    boolean includeEzzzy = true;
    boolean includeUndrgrnd = true;

    private boolean ongoingCall = false;
    private CallStateCallback phoneStateListener;
    private TelephonyManager telephonyManager;

    // Custom Commands
    public static final String ACTION_SKIP_NEXT = "ACTION_SKIP_NEXT";
    public static final String ACTION_SKIP_BACK = "ACTION_SKIP_BACK";
    public static final String ACTION_UPDATE_SETTINGS = "ACTION_UPDATE_SETTINGS";

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static class CallStateCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        private final MediaPlayerService service;

        public CallStateCallback(MediaPlayerService service) {
            this.service = service;
        }

        @Override
        public void onCallStateChanged(int state) {
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    if (service.player != null) {
                        service.player.pause();
                        service.ongoingCall = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (service.player != null) {
                        if (service.ongoingCall) {
                            service.ongoingCall = false;
                            service.player.play();
                        }
                    }
                    break;
            }
        }
    }

    @Nullable
    @Override
    public MediaLibrarySession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaLibrarySession;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (player != null) {
            player.pause();
            player.stop();
        }
        stopSelf();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onCreate() {
        super.onCreate();

        // Load initial disableMenuMusic setting from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("SaintsRadioPrefs", MODE_PRIVATE);
        disableMenuMusic = prefs.getBoolean("disableMenuMusic", false);

        // Configure Audio Attributes for Media Playback
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true) // true = handle audio focus
                .build();

        ForwardingPlayer forwardingPlayer = new ForwardingPlayer(player) {
            @Override
            public void seekToNext() {
                playNextInSequence();
            }

            @Override
            public void seekToNextMediaItem() {
                playNextInSequence();
            }

            @Override
            public void seekToPrevious() {
                seekTo(0);
            }

            @Override
            public void seekToPreviousMediaItem() {
                seekTo(0);
            }
        };

        // Custom buttons for Android Auto / Expanded notifications
        CommandButton skipNextButton = new CommandButton.Builder()
                .setDisplayName("Skip Next")
                .setIconResId(android.R.drawable.ic_media_next)
                .setSessionCommand(new SessionCommand(ACTION_SKIP_NEXT, Bundle.EMPTY))
                .build();

        MediaLibrarySession.Callback callback = new MediaLibrarySession.Callback() {
            @OptIn(markerClass = UnstableApi.class)
            @NonNull
            @Override
            public MediaSession.ConnectionResult onConnect(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
                MediaSession.ConnectionResult result = MediaLibrarySession.Callback.super.onConnect(session, controller);
                
                // Explicitly allow commands
                Player.Commands playerCommands = result.availablePlayerCommands.buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build();

                SessionCommands sessionCommands = result.availableSessionCommands.buildUpon()
                        .add(new SessionCommand(ACTION_SKIP_NEXT, Bundle.EMPTY))
                        .add(new SessionCommand(ACTION_SKIP_BACK, Bundle.EMPTY))
                        .add(new SessionCommand(ACTION_UPDATE_SETTINGS, Bundle.EMPTY))
                        .build();

                return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailablePlayerCommands(playerCommands)
                        .setAvailableSessionCommands(sessionCommands)
                        .build();
            }

            @NonNull
            @Override
            public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
                    @NonNull MediaLibrarySession session,
                    @NonNull MediaSession.ControllerInfo browser,
                    @Nullable MediaLibraryService.LibraryParams params) {
                
                Bundle extras = new Bundle();
                // Request Grid Layout for all levels in Android Auto
                extras.putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 2);
                extras.putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 2);
                
                MediaLibraryService.LibraryParams rootParams = new MediaLibraryService.LibraryParams.Builder()
                        .setExtras(extras)
                        .build();

                MediaItem root = new MediaItem.Builder()
                        .setMediaId("node_root")
                        .setMediaMetadata(new MediaMetadata.Builder()
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .build())
                        .build();
                return Futures.immediateFuture(LibraryResult.ofItem(root, rootParams));
            }

            @NonNull
            @Override
            public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
                    @NonNull MediaLibrarySession session,
                    @NonNull MediaSession.ControllerInfo browser,
                    @NonNull String parentId,
                    int page,
                    int pageSize,
                    @Nullable MediaLibraryService.LibraryParams params) {
                List<MediaItem> items = new ArrayList<>();
                if (parentId.equals("node_root")) {
                    items.add(createPlayableItem("saints", "Saints Radio", "saints_tile"));
                    items.add(createPlayableItem("krunch", "Krunch Radio", "krunch_tile"));
                    items.add(createPlayableItem("krhyme", "Krhyme Radio", "krhyme_tile"));
                    items.add(createPlayableItem("mix", "Mix Radio", "mix_tile"));
                    items.add(createPlayableItem("genx", "GenX Radio", "genx_tile"));
                    items.add(createPlayableItem("ezzzy", "Ezzzy Radio", "ezzzy_tile"));
                    items.add(createPlayableItem("undrgrnd", "Undrgrnd Radio", "undrgrnd_tile"));
                }
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params));
            }

            @NonNull
            @Override
            public ListenableFuture<List<MediaItem>> onAddMediaItems(
                    @NonNull MediaSession session,
                    @NonNull MediaSession.ControllerInfo controller,
                    @NonNull List<MediaItem> mediaItems) {
                if (!mediaItems.isEmpty()) {
                    MediaItem selected = mediaItems.get(0);
                    startRadio(selected.mediaId);
                }
                return Futures.immediateFuture(mediaItems);
            }

            @NonNull
            @Override
            public ListenableFuture<SessionResult> onCustomCommand(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller, @NonNull SessionCommand customCommand, @NonNull Bundle args) {
                switch (customCommand.customAction) {
                    case ACTION_SKIP_NEXT:
                        playNextInSequence();
                        return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
                    case ACTION_SKIP_BACK:
                        player.seekTo(0);
                        return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
                    case ACTION_UPDATE_SETTINGS:
                        commercialsPerSong = args.getInt("commercialsPerSong", commercialsPerSong);
                        songsBeforeNews = args.getInt("songsBeforeNews", songsBeforeNews);
                        includeSingAlongs = args.getBoolean("includeSingAlongs", includeSingAlongs);
                        
                        boolean oldDisableMenuMusic = disableMenuMusic;
                        disableMenuMusic = args.getBoolean("disableMenuMusic", disableMenuMusic);

                        includeKrunch = args.getBoolean("includeKrunch", includeKrunch);
                        includeKrhyme = args.getBoolean("includeKrhyme", includeKrhyme);
                        includeMix = args.getBoolean("includeMix", includeMix);
                        includeGenx = args.getBoolean("includeGenx", includeGenx);
                        includeEzzzy = args.getBoolean("includeEzzzy", includeEzzzy);
                        includeUndrgrnd = args.getBoolean("includeUndrgrnd", includeUndrgrnd);

                        // If menu music was just disabled and is currently playing, stop it.
                        if (disableMenuMusic && !oldDisableMenuMusic && currentStationId.isEmpty()) {
                            player.stop();
                        }

                        // Re-load media if preference changed and station is active
                        if (!currentStationId.isEmpty()) {
                            loadMedia(currentStationId);
                        }

                        return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
                }
                return MediaLibrarySession.Callback.super.onCustomCommand(session, controller, customCommand, args);
            }
        };

        mediaLibrarySession = new MediaLibrarySession.Builder(this, forwardingPlayer, callback)
                .setCustomLayout(ImmutableList.of(skipNextButton))
                .build();

        callStateListener();
        registerBecomingNoisyReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            register_startSaintsRadio();
            register_startKrunchRadio();
            register_startKrhymeRadio();
            register_startMixRadio();
            register_startGenxRadio();
            register_startEzzzyRadio();
            register_startUndrgrndRadio();
        }

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    playNextInSequence();
                }
            }
        });

        playPauseMenuMusic();
    }

    private void playPauseMenuMusic() {
        if (disableMenuMusic) return;

        @SuppressLint("DiscouragedApi")
        int resId = getResources().getIdentifier("shared_pausemenu", "raw", getPackageName());
        if (resId != 0) {
            MediaItem pauseItem = new MediaItem.Builder()
                    .setMediaId("shared_pausemenu")
                    .setUri("rawresource:///" + resId)
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setTitle("Pause Menu")
                            .setArtist("Stilwater Radio")
                            .build())
                    .build();
            player.setMediaItem(pauseItem);
            player.setRepeatMode(Player.REPEAT_MODE_ALL);
            player.prepare();
            player.play();
        }
    }

    private String formatSongTitle(String rawName) {
        if (rawName == null) return "Unknown Song";
        
        String title = rawName;
        if (title.contains("_")) {
            title = title.substring(title.indexOf("_") + 1);
        }
        
        // Remove suffixes first to avoid matching special cases incorrectly
        String suffix = "";
        if (title.endsWith("male1") || title.endsWith("male2") || title.endsWith("male3")) {
            suffix = " (Male Sing Along)";
            title = title.substring(0, title.length() - 5);
        } else if (title.endsWith("female1") || title.endsWith("female2") || title.endsWith("female3")) {
            suffix = " (Female Sing Along)";
            title = title.substring(0, title.length() - 7);
        }
        
        // General replacements
        title = title.replace("intro", "").replace("outro", "").replace("caller", "");
        
        // Special case formatting
        // Genx
        if (title.equalsIgnoreCase("allthativegot")) title = "All That I've Got";
        else if (title.equalsIgnoreCase("coatofarms")) title = "Coat Of Arms";
        else if (title.equalsIgnoreCase("facedown")) title = "Face Down";
        else if (title.equalsIgnoreCase("holeintheearth")) title = "Hole In The Earth";
        else if (title.equalsIgnoreCase("letmein")) title = "Let Me In";
        else if (title.equalsIgnoreCase("lyingisthemostfunagirlcanhavewithouttakingherclothesoff")) title = "Lying Is The Most Fun A Girl Can Have Without Taking Her Clothes Off";
        else if (title.equalsIgnoreCase("makedamnsure")) title = "Make Damn Sure";
        else if (title.equalsIgnoreCase("miserybusiness")) title = "Misery Business";
        else if (title.equalsIgnoreCase("putyourmoneywhereyourmouthis")) title = "Put Your Money Where Your Mouth Is";
        else if (title.equalsIgnoreCase("rockandrollqueen")) title = "Rock & Roll Queen";
        else if (title.equalsIgnoreCase("whatyouneed")) title = "What You Need";
        else if (title.equalsIgnoreCase("teenagers")) title = "Teenagers";
        // Krhyme
        else if (title.equalsIgnoreCase("gangstabitch")) title = "Gangsta Bitch";
        else if (title.equalsIgnoreCase("goodgirl")) title = "Good Girl";
        else if (title.equalsIgnoreCase("handsup")) title = "Hands Up";
        else if (title.equalsIgnoreCase("iluvit")) title = "I Luv It";
        else if (title.equalsIgnoreCase("meandu")) title = "Me & U";
        else if (title.equalsIgnoreCase("nystateofmind")) title = "N.Y. State Of Mind";
        else if (title.equalsIgnoreCase("onething")) title = "One Thing";
        else if (title.equalsIgnoreCase("ridininthatblackjoint")) title = "Ridin' In That Black Joint";
        else if (title.equalsIgnoreCase("sosick")) title = "So Sick";
        else if (title.equalsIgnoreCase("suckermcs")) title = "Sucker M.C.'s";
        else if (title.equalsIgnoreCase("tellmeboutit")) title = "Tell Me 'Bout It";
        else if (title.equalsIgnoreCase("trickme")) title = "Trick Me";
        else if (title.equalsIgnoreCase("whatathugabout")) title = "What A Thug About";
        // Krunch
        else if (title.equalsIgnoreCase("barnburner")) title = "Barn Burner";
        else if (title.equalsIgnoreCase("batcountry")) title = "Bat Country";
        else if (title.equalsIgnoreCase("colonyofbirchmen")) title = "Colony Of Birchmen";
        else if (title.equalsIgnoreCase("deadlysinners")) title = "Deadly Sinners";
        else if (title.equalsIgnoreCase("ghostofperdition")) title = "Ghost Of Perdition";
        else if (title.equalsIgnoreCase("milklizard")) title = "Milk Lizard";
        else if (title.equalsIgnoreCase("nothingleft")) title = "Nothing Left";
        else if (title.equalsIgnoreCase("whatahorriblenighttohaveacurse")) title = "What A Horrible Night To Have A Curse";
        else if (title.equalsIgnoreCase("anthem")) title = "Anthem";
        else if (title.equalsIgnoreCase("resurrection")) title = "Resurrection";
        else if (title.equalsIgnoreCase("stars")) title = "Stars";
        else if (title.equalsIgnoreCase("unsung")) title = "Unsung";
        else if (title.equalsIgnoreCase("woman")) title = "Woman";
        else if (title.equalsIgnoreCase("redneck")) title = "Redneck";
        // Mix
        else if (title.equalsIgnoreCase("dontyou")) title = "Don't You (Forget About Me)";
        else if (title.equalsIgnoreCase("takeonme")) title = "Take On Me";
        else if (title.equalsIgnoreCase("downunder")) title = "Down Under";
        else if (title.equalsIgnoreCase("outoftouch")) title = "Out Of Touch";
        else if (title.equalsIgnoreCase("thereflex")) title = "The Reflex";
        else if (title.equalsIgnoreCase("safetydance")) title = "The Safety Dance";
        else if (title.equalsIgnoreCase("prettyinpink")) title = "Pretty In Pink";
        else if (title.equalsIgnoreCase("workingfortheweekend")) title = "Working For The Weekend";
        else if (title.equalsIgnoreCase("everybodywantstoruletheworld")) title = "Everybody Wants To Rule The World";
        else if (title.equalsIgnoreCase("sisterchristian")) title = "Sister Christian";
        else if (title.equalsIgnoreCase("karmachameleon")) title = "Karma Chameleon";
        else if (title.equalsIgnoreCase("thefinalcountdown")) title = "The Final Countdown";
        // Ezzzy
        else if (title.equalsIgnoreCase("acielitolindo")) title = "A Cielito Lindo";
        else if (title.equalsIgnoreCase("agirllikeyou")) title = "A Girl Like You";
        else if (title.equalsIgnoreCase("asunnydayinheidelberg")) title = "A Sunny Day In Heidelberg";
        else if (title.equalsIgnoreCase("bachelorsamba")) title = "Bachelor Samba";
        else if (title.equalsIgnoreCase("bergundtal")) title = "Berg Und Tal";
        else if (title.equalsIgnoreCase("bossacubana")) title = "Bossa Cubana";
        else if (title.equalsIgnoreCase("chansonpourtoi")) title = "Chanson Pour Toi";
        else if (title.equalsIgnoreCase("coconuts")) title = "Coconuts";
        else if (title.equalsIgnoreCase("colonieceleste")) title = "Colonie Celeste";
        else if (title.equalsIgnoreCase("dancingontheavenue")) title = "Dancing On The Avenue";
        else if (title.equalsIgnoreCase("dolcevita")) title = "Dolce Vita";
        else if (title.equalsIgnoreCase("facetoface")) title = "Face To Face";
        else if (title.equalsIgnoreCase("jarabetapatio")) title = "Jarabe Tapatio";
        else if (title.equalsIgnoreCase("juststrollingalong")) title = "Just Strolling Along";
        else if (title.equalsIgnoreCase("kalamazoostyle")) title = "Kalamazoo Style";
        else if (title.equalsIgnoreCase("loveforlife")) title = "Love For Life";
        else if (title.equalsIgnoreCase("marvelloussingersremix")) title = "Marvellous Singers Remix";
        else if (title.equalsIgnoreCase("naughtybutnice")) title = "Naughty But Nice";
        else if (title.equalsIgnoreCase("stereochacha")) title = "Stereo Cha-Cha";
        else if (title.equalsIgnoreCase("swingpaname")) title = "Swing Paname";
        else if (title.equalsIgnoreCase("tchoupatwist")) title = "Tchoupa Twist";
        else if (title.equalsIgnoreCase("toobaboogie")) title = "Tooba Boogie";
        else if (title.equalsIgnoreCase("walkietalkie")) title = "Walkie Talkie";
        else if (title.equalsIgnoreCase("whistlehappy")) title = "Whistle Happy";
        // Undrgrnd
        else if (title.equalsIgnoreCase("andshewoulddarkenthememory")) title = "And She Would Darken The Memory";
        else if (title.equalsIgnoreCase("callinthedebts")) title = "Call In The Debts";
        else if (title.equalsIgnoreCase("cheeriton")) title = "Cheer It On";
        else if (title.equalsIgnoreCase("deadfriends")) title = "Dead Friends";
        else if (title.equalsIgnoreCase("dontcallitaghetto")) title = "Don't Call It A Ghetto";
        else if (title.equalsIgnoreCase("everysinglelinemeanssomething")) title = "Every Single Line Means Something";
        else if (title.equalsIgnoreCase("forreal")) title = "For Real";
        else if (title.equalsIgnoreCase("hazelst")) title = "Hazel St.";
        else if (title.equalsIgnoreCase("hearsyourfuture")) title = "Here's Your Future";
        else if (title.equalsIgnoreCase("houseofcards")) title = "House Of Cards";
        else if (title.equalsIgnoreCase("shoottherunner")) title = "Shoot The Runner";
        else if (title.equalsIgnoreCase("terror")) title = "Terror";
        else if (title.equalsIgnoreCase("thirdgearscratch")) title = "Third Gear Scratch";
        else if (title.equalsIgnoreCase("westernbiographic")) title = "Western Biographic";
        else if (!title.isEmpty()) {
            title = title.substring(0, 1).toUpperCase() + title.substring(1);
        }
        
        return title + suffix;
    }

    private MediaItem createPlayableItem(String id, String title, String iconName) {
        return new MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtworkUri(getUriForDrawable(iconName))
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build())
                .build();
    }

    @SuppressLint("DiscouragedApi")
    private Uri getUriForDrawable(String name) {
        if (name == null || name.isEmpty()) return null;
        int resId = getResources().getIdentifier(name, "drawable", getPackageName());
        if (resId == 0) return null;
        return Uri.parse("android.resource://" + getPackageName() + "/" + resId);
    }

    private void startRadio(String radio) {
        currentStationId = radio.toLowerCase();
        switch (currentStationId) {
            case "saints": currentStationName = "Saints Radio"; break;
            case "krunch": currentStationName = "Krunch Radio"; break;
            case "krhyme": currentStationName = "Krhyme Radio"; break;
            case "mix": currentStationName = "Mix Radio"; break;
            case "genx": currentStationName = "GenX Radio"; break;
            case "ezzzy": currentStationName = "Ezzzy Radio"; break;
            case "undrgrnd": currentStationName = "Undrgrnd Radio"; break;
        }
        
        player.stop();
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        loadMedia(currentStationId);
        
        // Start at a random point in the rotation
        int startChoice = random.nextInt(5);
        switch (startChoice) {
            case 0: soundType = "theme"; break;
            case 1: soundType = "intro"; break;
            case 2: soundType = "song"; break;
            case 3: soundType = "outro"; break;
            case 4: soundType = "commercial"; break;
        }
        
        playNextInSequence();
    }

    private MediaItem getMediaItemWithStationMetadata(String rawName, String itemTitle, String type) {
        @SuppressLint("DiscouragedApi")
        int resId = getResources().getIdentifier(rawName, "raw", getPackageName());
        String iconName = currentStationId + "_logo";
        
        String displayTitle = itemTitle;
        if (type != null && type.equals("song")) {
            displayTitle = formatSongTitle(rawName);
        }

        return new MediaItem.Builder()
                .setMediaId(rawName)
                .setUri("rawresource:///" + resId)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(displayTitle)
                        .setArtist(currentStationName)
                        .setArtworkUri(getUriForDrawable(iconName))
                        .build())
                .build();
    }

    @Override
    public void onDestroy() {
        if (mediaLibrarySession != null) {
            mediaLibrarySession.release();
        }
        if (player != null) {
            player.release();
        }
        super.onDestroy();
        if (phoneStateListener != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager.unregisterTelephonyCallback(phoneStateListener);
            }
        }
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(startSaintsRadio);
        unregisterReceiver(startKrunchRadio);
        unregisterReceiver(startKrhymeRadio);
        unregisterReceiver(startMixRadio);
        unregisterReceiver(startGenxRadio);
        unregisterReceiver(startEzzzyRadio);
        unregisterReceiver(startUndrgrndRadio);
    }

    private void loadMedia(String radio) {
        commercials.clear();
        news.clear();
        callers.clear();
        intros.clear();
        newsIntros.clear();
        outros.clear();
        themes.clear();
        songs.clear();
        songQueue.clear();
        Field[] fields = R.raw.class.getDeclaredFields();

        for (Field f : fields) {
            String name = f.getName();
            if (name.startsWith("shared_c")) {
                commercials.add(name);
            } else if (name.startsWith("shared_n")) {
                news.add(name);
            }
            
            if (Objects.equals(radio, "saints")) {
                if (name.startsWith("krunch_") && includeKrunch) categorizeFile(name);
                else if (name.startsWith("krhyme_") && includeKrhyme) categorizeFile(name);
                else if (name.startsWith("mix_") && includeMix) categorizeFile(name);
                else if (name.startsWith("genx_") && includeGenx) categorizeFile(name);
                else if (name.startsWith("ezzzy_") && includeEzzzy) categorizeFile(name);
                else if (name.startsWith("undrgrnd_") && includeUndrgrnd) categorizeFile(name);
            } else if (name.startsWith(radio.toLowerCase() + "_")) {
                categorizeFile(name);
            }
        }
        
        Collections.shuffle(commercials);
        Collections.shuffle(news);
        // We will shuffle the songs list but use a grouping logic when creating the queue
        Collections.shuffle(songs);
        Collections.shuffle(themes);
    }

    private void categorizeFile(String name) {
        if (name.contains("caller")) {
            callers.add(name);
        } else if (name.contains("intro") && !name.contains("news")) {
            intros.add(name);
        } else if (name.contains("news") && !name.startsWith("shared")) {
            newsIntros.add(name);
        } else if (name.contains("outro")) {
            outros.add(name);
        } else if (name.contains("_theme")) {
            themes.add(name);
        } else if (!name.startsWith("shared_")) {
            songs.add(name);
        }
    }

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            player.pause();
        }
    };

    private void registerBecomingNoisyReceiver() {
        IntentFilter intentFilter = new IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(becomingNoisyReceiver, intentFilter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(becomingNoisyReceiver, intentFilter);
        }
    }

    private void callStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            phoneStateListener = new CallStateCallback(this);
            telephonyManager.registerTelephonyCallback(this.getMainExecutor(), phoneStateListener);
        }
    }

    private void playNextInSequence() {
        if (songs.isEmpty()) return;

        if (Objects.equals(soundType, "theme")) {
            if (songQueue.isEmpty()) {
                generateSongQueue();
            }
            currentSongFile = songQueue.remove(0);

            if (currentSongFile.contains("_")) {
                songName = currentSongFile.substring(currentSongFile.indexOf("_") + 1);
            } else {
                songName = currentSongFile;
            }
            
            // For matching intros/outros, strip sing-along suffixes
            String matchName = songName;
            if (matchName.endsWith("male1") || matchName.endsWith("male2") || matchName.endsWith("male3")) {
                matchName = matchName.substring(0, matchName.length() - 5);
            } else if (matchName.endsWith("female1") || matchName.endsWith("female2") || matchName.endsWith("female3")) {
                matchName = matchName.substring(0, matchName.length() - 7);
            }
            
            introList.clear();
            for (String intro : intros) {
                if (intro.contains(matchName)) introList.add(intro);
            }
            for (String caller : callers) {
                if (caller.contains(matchName)) introList.add(caller);
            }
            for (String intro : intros) {
                if (intro.contains("_intro")) introList.add(intro);
            }
            
            if (!introList.isEmpty()) {
                Collections.shuffle(introList);
                player.setMediaItem(getMediaItemWithStationMetadata(introList.get(0), "Station Intro", "intro"));
                soundType = "intro";
            } else {
                soundType = "intro";
                playNextInSequence();
                return;
            }
        } else if (Objects.equals(soundType, "intro")) {
            player.setMediaItem(getMediaItemWithStationMetadata(currentSongFile, currentSongFile, "song"));
            newsItr = newsItr + 1;
            soundType = "song";
        } else if (Objects.equals(soundType, "song")) {
            // For matching intros/outros, strip sing-along suffixes
            String matchName = songName;
            if (matchName.endsWith("male1") || matchName.endsWith("male2") || matchName.endsWith("male3")) {
                matchName = matchName.substring(0, matchName.length() - 5);
            } else if (matchName.endsWith("female1") || matchName.endsWith("female2") || matchName.endsWith("female3")) {
                matchName = matchName.substring(0, matchName.length() - 7);
            }

            outroList.clear();
            for (String outro : outros) {
                if (outro.contains(matchName)) outroList.add(outro);
            }
            for (String outro : outros) {
                if (outro.contains("_outro")) outroList.add(outro);
            }
            
            if (!outroList.isEmpty()) {
                Collections.shuffle(outroList);
                player.setMediaItem(getMediaItemWithStationMetadata(outroList.get(0), "Station Outro", "outro"));
                soundType = "outro";
            } else {
                soundType = "outro";
                playNextInSequence();
                return;
            }
        } else if (Objects.equals(soundType, "outro") || (Objects.equals(soundType, "commercial") && (commItr < commercialsPerSong))) {
            if (commercialsPerSong > 0) {
                String commercial = commercials.get(random.nextInt(commercials.size()));
                player.setMediaItem(getMediaItemWithStationMetadata(commercial, "Commercial", "commercial"));
                commItr = commItr + 1;
                soundType = "commercial";
            } else {
                soundType = "commercial";
                commItr = 0;
                playNextInSequence();
                return;
            }
        } else if (Objects.equals(soundType, "commercial") && (newsItr >= songsBeforeNews)) {
            if (songsBeforeNews > 0) {
                Collections.shuffle(newsIntros);
                if (!newsIntros.isEmpty()) {
                    player.setMediaItem(getMediaItemWithStationMetadata(newsIntros.get(0), "News Intro", "newsIntro"));
                    soundType = "newsIntro";
                    newsItr = 0;
                } else {
                    soundType = "newsIntro";
                    playNextInSequence();
                    return;
                }
            } else {
                soundType = "news";
                playNextInSequence();
                return;
            }
        } else if (Objects.equals(soundType, "newsIntro")) {
            if (!news.isEmpty()) {
                String newsItem = news.get(random.nextInt(news.size()));
                player.setMediaItem(getMediaItemWithStationMetadata(newsItem, "News Report", "news"));
                soundType = "news";
            } else {
                soundType = "news";
                playNextInSequence();
                return;
            }
        } else {
            commItr = 0;
            if (!themes.isEmpty()) {
                String theme = themes.get(random.nextInt(themes.size()));
                player.setMediaItem(getMediaItemWithStationMetadata(theme, "Station Theme", "theme"));
                soundType = "theme";
            } else {
                soundType = "theme";
                playNextInSequence();
                return;
            }
        }
        player.prepare();
        player.play();
    }

    private void generateSongQueue() {
        songQueue.clear();
        Map<String, List<String>> songGroups = new HashMap<>();
        
        for (String song : songs) {
            String baseName = song;
            if (baseName.endsWith("male1") || baseName.endsWith("male2") || baseName.endsWith("male3")) {
                baseName = baseName.substring(0, baseName.length() - 5);
            } else if (baseName.endsWith("female1") || baseName.endsWith("female2") || baseName.endsWith("female3")) {
                baseName = baseName.substring(0, baseName.length() - 7);
            }
            
            if (!songGroups.containsKey(baseName)) {
                songGroups.put(baseName, new ArrayList<>());
            }
            Objects.requireNonNull(songGroups.get(baseName)).add(song);
        }
        
        List<String> keys = new ArrayList<>(songGroups.keySet());
        Collections.shuffle(keys);
        
        for (String key : keys) {
            List<String> variants = songGroups.get(key);
            if (!includeSingAlongs) {
                // Only find the non-singalong version (the one that doesn't have suffix)
                if (variants != null) {
                    for (String v : variants) {
                        if (!v.endsWith("male1") && !v.endsWith("male2") && !v.endsWith("male3") &&
                            !v.endsWith("female1") && !v.endsWith("female2") && !v.endsWith("female3")) {
                            songQueue.add(v);
                            break;
                        }
                    }
                }
            } else {
                // Randomly pick one from variants
                songQueue.add(Objects.requireNonNull(variants).get(random.nextInt(variants.size())));
            }
        }
    }

    private final BroadcastReceiver startSaintsRadio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startRadio("saints");
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void register_startSaintsRadio() {
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_START_SAINTS_RADIO);
        registerReceiver(startSaintsRadio, filter, RECEIVER_EXPORTED);
    }

    private final BroadcastReceiver startKrunchRadio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startRadio("krunch");
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void register_startKrunchRadio() {
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_START_KRUNCH_RADIO);
        registerReceiver(startKrunchRadio, filter, RECEIVER_EXPORTED);
    }

    private final BroadcastReceiver startKrhymeRadio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startRadio("krhyme");
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void register_startKrhymeRadio() {
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_START_KRHYME_RADIO);
        registerReceiver(startKrhymeRadio, filter, RECEIVER_EXPORTED);
    }

    private final BroadcastReceiver startMixRadio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startRadio("mix");
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void register_startMixRadio() {
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_START_MIX_RADIO);
        registerReceiver(startMixRadio, filter, RECEIVER_EXPORTED);
    }

    private final BroadcastReceiver startGenxRadio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startRadio("genx");
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void register_startGenxRadio() {
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_START_GENX_RADIO);
        registerReceiver(startGenxRadio, filter, RECEIVER_EXPORTED);
    }

    private final BroadcastReceiver startEzzzyRadio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startRadio("ezzzy");
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void register_startEzzzyRadio() {
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_START_EZZZY_RADIO);
        registerReceiver(startEzzzyRadio, filter, RECEIVER_EXPORTED);
    }

    private final BroadcastReceiver startUndrgrndRadio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startRadio("undrgrnd");
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void register_startUndrgrndRadio() {
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_START_UNDRGRND_RADIO);
        registerReceiver(startUndrgrndRadio, filter, RECEIVER_EXPORTED);
    }
}
