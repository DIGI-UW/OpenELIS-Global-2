package org.openelisglobal.orderentry.valueholder;

import java.time.LocalDate;

/**
 * One existing patient, facility or provider read as a possible duplicate of a
 * record being created (FR-B6a). Only the fields the check compares or shows
 * are carried; the similarity values come from PostgreSQL {@code pg_trgm} and
 * {@code fuzzystrmatch}.
 */
public class PossibleMatchCandidate {

    private String id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String identifier;
    private String name;
    private String code;
    private String city;
    private boolean active = true;
    private double lastToLast;
    private double firstToFirst;
    private double lastToFirst;
    private double firstToLast;
    private boolean lastSoundsAlike;
    private boolean firstSoundsAlike;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getLastToLast() {
        return lastToLast;
    }

    public void setLastToLast(double lastToLast) {
        this.lastToLast = lastToLast;
    }

    public double getFirstToFirst() {
        return firstToFirst;
    }

    public void setFirstToFirst(double firstToFirst) {
        this.firstToFirst = firstToFirst;
    }

    public double getLastToFirst() {
        return lastToFirst;
    }

    public void setLastToFirst(double lastToFirst) {
        this.lastToFirst = lastToFirst;
    }

    public double getFirstToLast() {
        return firstToLast;
    }

    public void setFirstToLast(double firstToLast) {
        this.firstToLast = firstToLast;
    }

    public boolean isLastSoundsAlike() {
        return lastSoundsAlike;
    }

    public void setLastSoundsAlike(boolean lastSoundsAlike) {
        this.lastSoundsAlike = lastSoundsAlike;
    }

    public boolean isFirstSoundsAlike() {
        return firstSoundsAlike;
    }

    public void setFirstSoundsAlike(boolean firstSoundsAlike) {
        this.firstSoundsAlike = firstSoundsAlike;
    }
}
