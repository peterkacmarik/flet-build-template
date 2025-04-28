# android/app/src/main/python/service/main.py
import time
from foreground_service import StopwatchService

def main():
    """
    Toto volá stub v onStartCommand().
    Tu spúšťaš svoj Python kód služby.
    """
    svc = StopwatchService()
    svc.start_foreground_service()
    svc.start_timer()
    # Udrž vlákno nažive, aby Pyth­on proces bežal
    while True:
        time.sleep(1)
