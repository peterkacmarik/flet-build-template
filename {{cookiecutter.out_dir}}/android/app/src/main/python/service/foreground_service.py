# # services/foreground_service.py

# from jnius import autoclass
# from time import sleep

# def main():
#     PythonService = autoclass("org.kivy.android.PythonService")
#     service = PythonService.mService

#     NotificationBuilder = autoclass("android.app.Notification$Builder")
#     notification = NotificationBuilder(service) \
#         .setContentTitle("Stopwatch") \
#         .setContentText("Running in background...") \
#         .setSmallIcon(service.getApplicationInfo().icon) \
#         .build()

#     service.startForeground(1, notification)

#     count = 0
#     while True:
#         print(f"Stopwatch running: {count} seconds")
#         sleep(1)
#         count += 1




# foreground_service.py
import os
import time
import threading
import traceback
from jnius import cast, autoclass, PythonJavaClass, java_method
import flet as ft
from service.notification import send_notification


# Android classes needed for foreground service
if hasattr(__builtins__, 'SDK_INT'):
    # PythonActivity = autoclass('io.flet.app.FletActivity')
    # PythonActivity = autoclass('io.flet.android.FletActivity')
    PythonActivity = autoclass('org.kivy.android.PythonActivity')
    Context = autoclass('android.content.Context')
    Intent = autoclass('android.content.Intent')
    PendingIntent = autoclass('android.app.PendingIntent')
    NotificationManager = autoclass('android.app.NotificationManager')
    NotificationChannel = autoclass('android.app.NotificationChannel')
    Notification = autoclass('android.app.Notification')
    NotificationBuilder = autoclass('android.app.Notification$Builder')
    Build = autoclass('android.os.Build')
    Service = autoclass('android.app.Service')
    # PythonService = autoclass('org.kivy.android.PythonService')
    IntentFilter = autoclass('android.content.IntentFilter')

class StopwatchReceiver(PythonJavaClass):
    __javainterfaces__ = ['android/content/BroadcastReceiver']
    
    def __init__(self, callback):
        super().__init__()
        self.callback = callback
    
    @java_method('(Landroid/content/Context;Landroid/content/Intent;)V')
    def onReceive(self, context, intent):
        if intent.getAction() == "STOP_STOPWATCH":
            self.callback("stop")

class StopwatchService:
    def __init__(self):
        self.running = False
        self.start_time = 0
        self.elapsed_time = 0
        self.timer_thread = None
        self.callback = None
        self.notification_manager = None
        self.notification_builder = None
        self.service_running = False
        self.receiver = None

    def format_time(self, seconds):
        """Formátuje uplynutý čas na HH:MM:SS."""
        hours = int(seconds // 3600)
        minutes = int((seconds % 3600) // 60)
        secs = int(seconds % 60)
        return f"{hours:02}:{minutes:02}:{secs:02}"

    def start_timer(self):
        """Start the stopwatch timer"""
        if not self.running:
            self.running = True
            self.start_time = time.time() - self.elapsed_time
            self.timer_thread = threading.Thread(target=self._timer_loop)
            self.timer_thread.daemon = True
            self.timer_thread.start()
            
            if self.callback:
                self.callback({
                    "time": self.format_time(self.elapsed_time),
                    "isStopWatchRunning": self.running
                })
    
    def stop_timer(self):
        """Stop the stopwatch timer"""
        if self.running:
            self.running = False
            self.elapsed_time = time.time() - self.start_time
            
            # Odoslanie jednoduchej notifikácie pri zastavení
            self.notify_completion()
            
            if self.callback:
                self.callback({
                    "time": self.format_time(self.elapsed_time),
                    "isStopWatchRunning": self.running
                })
    
    def reset_timer(self):
        """Reset the stopwatch timer"""
        self.stop_timer()
        self.elapsed_time = 0
        
        if self.callback:
            self.callback({
                "time": self.format_time(self.elapsed_time),
                "isStopWatchRunning": self.running
            })
    
    def _timer_loop(self):
        """Internal timer loop that updates elapsed time"""
        while self.running:
            self.elapsed_time = time.time() - self.start_time
            current_time = self.format_time(self.elapsed_time)
            
            # Update notification with current time
            if self.service_running and self.notification_builder:
                self.update_notification(f"Čas beží: {current_time}")
            
            if self.callback:
                self.callback({
                    "time": current_time,
                    "isStopWatchRunning": self.running
                })
            time.sleep(0.1)  # Update 10 times per second
    
    def set_callback(self, callback):
        """Set callback function for timer updates"""
        self.callback = callback
    
    

    def start_foreground_service(self):
        """Start the foreground service—and its notification—on Android."""
        # 1) Make sure we’re actually on Android by trying to import the Kivy Activity
        try:
            PythonActivity = autoclass('org.kivy.android.PythonActivity')
        except Exception:
            print("Not running on Android, skipping foreground service")
            return False

        activity = PythonActivity.mActivity
        context = cast('android.content.Context', activity)

        # 2) Create your notification channel if needed
        NotificationManager = autoclass('android.app.NotificationManager')
        NotificationChannel = autoclass('android.app.NotificationChannel')
        channel_id = "stopwatch_channel"
        channel_name = "Stopwatch Channel"
        importance = NotificationManager.IMPORTANCE_HIGH

        manager = activity.getSystemService(Context.NOTIFICATION_SERVICE)
        if Build.VERSION.SDK_INT >= 26:
            channel = NotificationChannel(channel_id, channel_name, importance)
            channel.enableVibration(True)
            channel.setVibrationPattern([100, 200, 300, 400, 500])
            manager.createNotificationChannel(channel)

        # 3) Build the notification
        NotificationBuilder = autoclass('android.app.Notification$Builder')
        if Build.VERSION.SDK_INT >= 26:
            nb = NotificationBuilder(context, channel_id)
        else:
            nb = NotificationBuilder(context)

        nb.setContentTitle("Stopky")
        nb.setContentText("Čas beží: 00:00:00")
        nb.setSmallIcon(activity.getApplicationInfo().icon)
        nb.setOngoing(True)

        notification = nb.build()

        # 4) Finally, call startForeground **on the Service** itself
        PythonService = autoclass('org.kivy.android.PythonService')
        python_service = PythonService.mService
        python_service.startForeground(1001, notification)

        # 5) Register your STOP broadcast receiver
        self.receiver = StopwatchReceiver(self)
        IntentFilter = autoclass('android.content.IntentFilter')
        filter = IntentFilter("STOP_STOPWATCH")
        context.registerReceiver(self.receiver, filter)

        self.notification_manager = manager
        self.notification_builder = nb
        self.service_running = True
        return True

    # def start_foreground_service(self):
    #     """Start the foreground service on Android"""
    #     if not hasattr(__builtins__, 'SDK_INT'):
    #         print("Not running on Android, foreground service not available")
    #         return False
        
    #     if self.service_running:
    #         return True
        
    #     try:
    #         PythonActivity = autoclass('org.kivy.android.PythonActivity')
    #         activity = PythonActivity.mActivity
    #         context = cast('android.content.Context', activity)
    #         self.service_running = True
            
    #         # Create notification channel (required for Android 8.0+)
    #         channel_id = "stopwatch_channel"
    #         channel_name = "Stopwatch Channel"
    #         importance = NotificationManager.IMPORTANCE_HIGH  # Zmena na HIGH pre lepšiu viditeľnosť
            
    #         if Build.VERSION.SDK_INT >= 26:
    #             channel = NotificationChannel(
    #                 channel_id,
    #                 channel_name,
    #                 importance
    #             )
    #             # Nastaviť zvuk a vibrácie pre lepšiu viditeľnosť notifikácie
    #             channel.enableVibration(True)
    #             channel.setVibrationPattern([100, 200, 300, 400, 500])
    #             channel.setShowBadge(True)
                
    #             self.notification_manager = activity.getSystemService(
    #                 Context.NOTIFICATION_SERVICE
    #             )
    #             self.notification_manager.createNotificationChannel(channel)
    #         else:
    #             self.notification_manager = activity.getSystemService(
    #                 Context.NOTIFICATION_SERVICE
    #             )
            
    #         # Create intent for when notification is clicked
    #         intent = Intent(activity, PythonActivity)
    #         intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
    #         pending_intent = PendingIntent.getActivity(
    #             context, 0, intent, 
    #             PendingIntent.FLAG_IMMUTABLE if Build.VERSION.SDK_INT >= 23 else 0
    #         )
            
    #         # Create stop button for notification
    #         stop_intent = Intent("STOP_STOPWATCH")
    #         stop_pending_intent = PendingIntent.getBroadcast(
    #             context, 0, stop_intent, 
    #             PendingIntent.FLAG_IMMUTABLE if Build.VERSION.SDK_INT >= 23 else 0
    #         )
            
    #         # Create notification
    #         if Build.VERSION.SDK_INT >= 26:
    #             self.notification_builder = NotificationBuilder(context, channel_id)
    #         else:
    #             self.notification_builder = NotificationBuilder(context)
            
    #         self.notification_builder.setContentTitle("Stopky")
    #         self.notification_builder.setContentText("Čas beží: 00:00:00")
    #         self.notification_builder.setContentIntent(pending_intent)
    #         self.notification_builder.setSmallIcon(activity.getApplicationInfo().icon)
    #         self.notification_builder.setOngoing(True)
            
    #         # Pridať prioritu pre staršie verzie Androidu
    #         if Build.VERSION.SDK_INT < 26:
    #             self.notification_builder.setPriority(NotificationManager.IMPORTANCE_HIGH)
            
    #         # Add stop action to notification
    #         if Build.VERSION.SDK_INT >= 16:
    #             self.notification_builder.addAction(
    #                 activity.getApplicationInfo().icon,
    #                 "Zastaviť",
    #                 stop_pending_intent
    #             )
            
    #         # Pridať notifikáciu PRED zavolaním startForeground
    #         notification = self.notification_builder.build()
            
    #         # Použiť service context namiesto activity
    #         current_service = activity.getApplicationContext()
            
    #         if Build.VERSION.SDK_INT >= 26:
    #             # activity.startForeground(1001, notification)
    #             ServiceClass = autoclass('org.kivy.android.PythonService')  # alebo názov tvojej generated service
    #             intent = Intent(context, ServiceClass)
    #             # Android 8.0+ vyžaduje startForegroundService
    #             if Build.VERSION.SDK_INT >= 26:
    #                 context.startForegroundService(intent)
    #             else:
    #                 context.startService(intent)
    #         else:
    #             self.notification_manager.notify(1001, notification)
            
    #         # Register receiver for stop button
    #         self.receiver = StopwatchReceiver(self)
    #         filter = IntentFilter()
    #         filter.addAction("STOP_STOPWATCH")
    #         context.registerReceiver(self.receiver, filter)
            
    #         return True
            
    #     except Exception as e:
    #         print(f"Chyba pri štarte foreground služby: {e}")
    #         traceback.print_exc()
    #         self.service_running = False
    #         return False
    
    def update_notification(self, text):
        """Update the notification text"""
        if self.notification_builder and self.notification_manager:
            try:
                self.notification_builder.setContentText(text)
                notification = self.notification_builder.build()
                self.notification_manager.notify(1001, notification)
                
                # Pre správnu funkciu foreground služby musíme updatovať
                # aj samotnú foreground notifikáciu
                if hasattr(__builtins__, 'SDK_INT'):
                    activity = PythonActivity.mActivity
                    if Build.VERSION.SDK_INT >= 26:
                        try:
                            activity.startForeground(1001, notification)
                        except Exception as e:
                            print(f"Chyba pri aktualizácii foreground notifikácie: {e}")
                
            except Exception as e:
                print(f"Chyba pri aktualizácii notifikácie: {e}")
    
    def stop_foreground_service(self):
        """Stop the foreground service"""
        if not self.service_running:
            return
            
        if hasattr(__builtins__, 'SDK_INT'):
            try:
                activity = PythonActivity.mActivity
                
                # Unregister the receiver
                if self.receiver:
                    context = cast('android.content.Context', activity)
                    try:
                        context.unregisterReceiver(self.receiver)
                    except Exception as e:
                        print(f"Chyba pri odregistrovaní receiveru: {e}")
                    self.receiver = None
                
                # Stop foreground service
                if Build.VERSION.SDK_INT >= 24:
                    activity.stopForeground(Service.STOP_FOREGROUND_REMOVE)
                else:
                    activity.stopForeground(True)
                
                # Zrušiť notifikáciu manuálne pre istotu
                if self.notification_manager:
                    self.notification_manager.cancel(1001)
                
            except Exception as e:
                print(f"Chyba pri zastavení foreground služby: {e}")
                traceback.print_exc()
                
        self.service_running = False
        self.stop_timer()


    def send_notification(title, text, status_text=None):
        try:
            # priamo získame current Activity
            PythonActivity = autoclass('org.kivy.android.PythonActivity')
            activity = PythonActivity.mActivity
            # Prístup k Android notifikačnému manažérovi
            Context = autoclass('android.content.Context')
            NotificationManager = autoclass('android.app.NotificationManager')
            NotificationChannel = autoclass('android.app.NotificationChannel')
            Notification = autoclass('android.app.Notification')
            NotificationBuilder = autoclass('android.app.Notification$Builder')
            # Build = autoclass('android.os.Build')

            # Spustenie notifikačnej služby
            notification_service = activity.getSystemService(Context.NOTIFICATION_SERVICE)

            # Vytvorenie notifikačného kanála (pre Android 8.0 a vyššie)
            channel_id = "my_notification_channel"
            channel_name = "Flet Notification Channel"
            importance = NotificationManager.IMPORTANCE_DEFAULT

            # Vytvorenie notifikačného kanála (Android 8.0+)
            channel = NotificationChannel(channel_id, channel_name, importance)
            notification_service.createNotificationChannel(channel)
            
            # Vytvorenie notifikácie
            builder = NotificationBuilder(activity, channel_id)
            builder.setContentTitle(title)
            builder.setContentText(text)
            builder.setSmallIcon(activity.getApplicationInfo().icon)
            builder.setAutoCancel(True)  # Notifikácia zmizne po kliknutí

            # Zobrazenie notifikácie
            notification_id = 1
            notification = builder.build()
            notification_service.notify(notification_id, notification)

            # Aktualizácia stavového textu na indikáciu úspechu
            if status_text:
                status_text.value = "✅ Notifikácia úspešne odoslaná."
                status_text.color = "green"
            
            return True

        except Exception as ex:
            # Aktualizácia stavového textu na indikáciu chyby
            # Update status text to indicate failure
            # status_text.value = f"❗ Failed to send notification: {traceback.format_exc()}"
            # status_text.color = "red"
            # return False
            
            error_msg = f"❗ Nepodarilo sa odoslať notifikáciu: {str(ex)}"
            if status_text:
                status_text.value = error_msg
                status_text.color = "red"
            print(error_msg)
            traceback.print_exc()
            return False
        
        
    

    # Notification methods for Android
    def notify_completion(self, status_text=None):
        """
        Odošle jednoduchú notifikáciu o dokončení časovača
        """
        elapsed_time = self.format_time(self.elapsed_time)
        send_notification(
            "Časovač dokončený", 
            f"Konečný čas: {elapsed_time}", 
            status_text
        )



# Example application that uses the stopwatch service
# class StopwatchApp:
#     def __init__(self):
#         self.stopwatch_service = StopwatchService()
#         self.page = None
        
#     def main(self, page: ft.Page):
#         self.page = page
#         page.title = "Stopwatch Demo"
#         page.theme_mode = ft.ThemeMode.LIGHT
#         page.padding = 20
        
#         # Initialize stopwatch display
#         self.time_display = ft.Text(
#             value="00:00:00",
#             size=40,
#             weight=ft.FontWeight.BOLD,
#             text_align=ft.TextAlign.CENTER
#         )
        
#         # Setup callback from stopwatch service to UI
#         def on_timer_update(data):
#             # Use page.update instead of add_to_control_update_batch
#             if self.page:
#                 self.time_display.value = data["time"]
                
#                 # Update button visibility based on timer state
#                 if data["isStopWatchRunning"]:
#                     start_button.visible = False
#                     stop_button.visible = True
#                     reset_button.visible = False
#                 else:
#                     start_button.visible = True
#                     stop_button.visible = False
#                     reset_button.visible = True
                
#                 # Update notification if service is running
#                 if self.stopwatch_service.service_running:
#                     self.stopwatch_service.update_notification(f"Čas beží: {data['time']}")
                
#                 # Update UI
#                 self.page.update()
        
#         self.stopwatch_service.set_callback(on_timer_update)
        
#         # Create start button
#         start_button = ft.IconButton(
#             icon=ft.Icons.PLAY_CIRCLE,
#             icon_size=60,
#             icon_color=ft.Colors.GREEN,
#             on_click=self.start_stopwatch
#         )
        
#         # Create stop button (initially hidden)
#         stop_button = ft.IconButton(
#             icon=ft.Icons.STOP_CIRCLE,
#             icon_size=60,
#             icon_color=ft.Colors.RED,
#             visible=False,
#             on_click=self.stop_stopwatch
#         )
        
#         # Create reset button
#         reset_button = ft.IconButton(
#             icon=ft.Icons.RESTART_ALT,
#             icon_size=60,
#             icon_color=ft.Colors.BLUE,
#             on_click=self.reset_stopwatch
#         )
        
#         # Create a card to display the time
#         time_card = ft.Card(
#             content=ft.Container(
#                 content=self.time_display,
#                 padding=10,
#                 alignment=ft.alignment.center
#             ),
#             elevation=10
#         )
        
#         # Add everything to the page
#         page.add(
#             ft.Column(
#                 controls=[
#                     time_card,
#                     ft.Row(
#                         [start_button, stop_button, reset_button],
#                         alignment=ft.MainAxisAlignment.CENTER
#                     )
#                 ],
#                 horizontal_alignment=ft.CrossAxisAlignment.CENTER,
#                 alignment=ft.MainAxisAlignment.CENTER,
#                 spacing=20
#             )
#         )
    
#     def start_stopwatch(self, e):
#         """Start the stopwatch and foreground service"""
#         self.stopwatch_service.start_foreground_service()
#         self.stopwatch_service.start_timer()
    
#     def stop_stopwatch(self, e):
#         """Stop the stopwatch and foreground service"""
#         self.stopwatch_service.stop_timer()
#         self.stopwatch_service.stop_foreground_service()
        
#     def reset_stopwatch(self, e):
#         """Reset the stopwatch and foreground service"""
#         self.stopwatch_service.reset_timer()
#         self.stopwatch_service.stop_foreground_service()
        
#     def run(self):
#         """Run the application"""
#         ft.app(target=self.main)

# if __name__ == "__main__":
#     app = StopwatchApp()
#     app.run()






