package tech.ypsilon.bbbot.discord.services;

import net.dv8tion.jda.api.entities.TextChannel;
import tech.ypsilon.bbbot.ButterBrot;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class GuildNotifierService {

    private final TextChannel channel;

    public GuildNotifierService(TextChannel channel) {
        this.channel = channel;
    }

    public TextChannel getChannel() {
        return this.channel;
    }

    public abstract void execute(TextChannel channel);

    public abstract NotifyTime getNotifyTime();

    public abstract String getServiceName();

    public void startService() {
        ButterBrot.LOGGER.info(String.format("[%s]: Registering the notification-service", this.getServiceName()));

        NotifyTime notifyTime = this.getNotifyTime();


        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);
        long delay = notifyTime.getStartHour() * 60L;
        if (hour < notifyTime.getStartHour()) {
            delay = ((notifyTime.getStartHour() - 1) - hour) * 60L + (60 - min);
        } else {
            delay += (23 - hour) * 60L + (60 - min);
        }

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(3);
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {
            try {
                this.execute(this.getChannel());
            } catch (Exception e) {
                System.err.println("Error while notifying the birthdays :(");
            }
        }, notifyTime.getSecondDelay(), notifyTime.getSecondInterval(), TimeUnit.SECONDS);

        ButterBrot.LOGGER.info(String.format("[%s]: Registered the notification-service", this.getServiceName()));
    }

    public static class NotifyTime {

        private int startHour;
        private int startMinute;
        private int startSecond;
        private int secondInterval;

        public static final int HOURLY = 60 * 60;
        public static final int DAILY = 60 * 60 * 24;

        public NotifyTime(int startHour, int startMinute, int startSecond, int secondInterval) {
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.startSecond = startSecond;
            this.secondInterval = secondInterval;
        }

        public NotifyTime setStartHour(int hour) {
            this.startHour = hour;
            return this;
        }

        public NotifyTime setStartMinute(int minute) {
            this.startMinute = minute;
            return this;
        }

        public NotifyTime setStartSecond(int second) {
            this.startSecond = second;
            return this;
        }

        public NotifyTime setStartInterval(int secondInterval) {
            this.secondInterval = secondInterval;
            return this;
        }

        public int getStartHour() {
            return startHour;
        }

        public int getStartMinute() {
            return startMinute;
        }

        public int getStartSecond() {
            return startSecond;
        }

        public int getSecondInterval() {
            return secondInterval;
        }

        public int getSecondDelay() {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int minute = Calendar.getInstance().get(Calendar.MINUTE);
            int second = Calendar.getInstance().get(Calendar.SECOND);

            return delayTo(hour, minute, second);
        }

        private int delayTo(int hour, int minute, int second){
            int dHour = (this.getStartHour() - hour);
            int dMin = (this.getStartMinute() - minute);
            int dSec = (this.getStartSecond() - second);

            int time = this.getStartHour() * 60 * 60 + this.getStartMinute() * 60 + this.getStartSecond();

            if (dHour < 0 || (dHour == 0 && dMin < 0) || (dHour == 0 && dMin == 0 && dSec < 0)) {
                // Next day
                return time + (23 - hour) * 60 * 60 + (59 - minute) * 60 + (59 - second);
            } else {
                // Same day.
                return (dHour * 60 * 60) + (dMin * 60) + (dSec);
            }
        }

    }


}