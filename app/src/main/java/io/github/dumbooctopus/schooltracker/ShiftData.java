package io.github.dumbooctopus.schooltracker;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by neilprajapati on 6/3/18.
 */


public class ShiftData implements Serializable{
    private static final long serialVersionUID = 4L + 1; //change whenever you change class

    public enum ShiftDataState{
        APPROVED, UNVERIFIED, DISPUTED, DISPUTE_ACCEPTED //dispute approved = future feature
    }

    private Date start;
    private Date end;
    private String location;
    private ShiftDataState state;
    private String disputeMessage;

    public ShiftData(String location, Date start){
        this.start = start;
        this.end = null;
        this.location = location;
        this.state = ShiftDataState.UNVERIFIED;
    }

    public void setEnd(Date end){
        this.end = end;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public String getLocation() {
        return location;
    }

    public ShiftDataState getState(){return state;}
    /**
     *
     * @return the difference between start and end in hours
     */
    public String getDurationString(){
        double roundedHours = computeRoundedHours(start, (end==null?Calendar.getInstance().getTime():end));
        int minutes =  (int)(60 * (roundedHours - Math.floor(roundedHours)));

        if(roundedHours < 1)
            return (end==null?"+":"")  + minutes + " m";
        return (end==null?"+":"") + Math.floor(roundedHours)+" h\n" + minutes + " m";
    }

    public String toString(){
        return start + " to " + (end == null?"IN Progress":end);
    }

    public String getStartString(){
        SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/d h:mm a", Locale.ENGLISH);
        return ""+dateFormatter.format(start);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShiftData shiftData = (ShiftData) o;

        if (!start.equals(shiftData.start)) return false;
        if (end != null ? !end.equals(shiftData.end) : shiftData.end != null) return false;
        return location.equals(shiftData.location);

    }

    @Override
    public int hashCode() {
        int result = start.hashCode();
        result = 31 * result + (end != null ? end.hashCode() : 0);
        result = 31 * result + location.hashCode();
        return result;
    }


    public void approve(){
        state = ShiftDataState.APPROVED;
    }

    public void dispute(String disputeMessage){
        this.disputeMessage = disputeMessage;
        state = ShiftDataState.DISPUTED;
    }

    //=======================================UTILITY METHODS==========================//
    public static double computeRoundedHours(Date start, Date end){
        long diff = end.getTime() - start.getTime();
        double hours =  (diff / 1000.0  / 60.0 / 60);

        //only keep .25, .5, .75,
        double decimal = hours - Math.floor(hours);
        double finalDecimal;
        if(decimal <= 0.125){
            finalDecimal = 0;
        } else if(decimal <= 0.375){
            finalDecimal = 0.25;
        } else if(decimal <= 0.625){
            finalDecimal = 0.5;
        } else if(decimal <= 0.875){
            finalDecimal = 0.75;
        } else{
            finalDecimal = 1;
        }
        return Math.floor(hours) + finalDecimal;
    }
}
