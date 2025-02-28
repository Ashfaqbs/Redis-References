package com.example.demo.service;


import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Note;
import com.example.demo.repo.NoteRepo;

@Service
public class NoteService {

    @Autowired
    private NoteRepo noteRepository;
    @Autowired
     private CacheManager cacheManager;



    //  no cache for this method
//     When to Cache getAllNotes()?
// ✅ Cache if:

// The notes do not change frequently.
// The list is expensive to compute (e.g., complex joins, heavy queries).
// You have a large dataset and want to reduce database load.


// ❌ Do not cache if:

// Notes are updated or inserted frequently.
// The list changes often, making cached data stale.
// The query is already fast (e.g., simple SELECT * on a small table).
    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }


    @Cacheable(value = "note", key = "#noteId") //this will check now if the data for the given id is present in cache or not
    // if present then it will return the data from cache else it will call the method and store the data in cache
    // here note is the cache name and noteId is the key and the cache name can be anything
    // so noteId coming from api to service will be the key for the cache 
    // for id 1 is called from api to service then it will be stored in cache with key 1 , if again id 1 is called from api to service then it will return the data from cache
    // but first time we fetch the data from db and store it in cache
   // similarly for id 2 and so on...... 
//  We can get this data from redis-cli by using the below command
/*
 
GET note::55 
"\xac\xed\x00\x05sr\x00\x1ccom.example.demo.entity.Note\x00\x00\x00\x00\x00\x00\x00\x01\x02\x00\x05Z\x00\x06isLiveL\x00\taddedDatet\x00\x10Ljava/util/
Date;L\x00\acontentt\x00\x12Ljava/lang/String;L\x00\x02idt\x00\x10Ljava/lang/Long;L\x00\x05titleq\x00~\x00\x02xp\x01sr\x00\x12java.sql.Timestamp&\x18\
xd5\xc8\x01S\xbfe\x02\x00\x01I\x00\x05nanosxr\x00\x0ejava.util.Datehj\x81\x01KYt\x19\x03\x00\x00xpw\b\x00\x00\x01\x95I\xdaT\x00x\x00\x00\x00\x00t\x00\x16
This is a sample note.sr\x00\x0ejava.lang.Long;\x8b\xe4\x90\xcc\x8f#\xdf\x02\x00\x01J\x00\x05valuexr\x00\x10java.lang.Number\x86\xac\x95\x1d\x0b\x94\xe0\x8b\
x02\x00\x00xp\x00\x00\x00\x00\x00\x00\x007t\x00\x0bSample Note"

 */

  /*
     OP

http://localhost:8080/api/notes/55

     {
    "id": 55,
    "title": "Sample Note",
    "content": "This is a sample note.",
    "addedDate": "2025-02-28T00:00:00.000+00:00",
    "live": true
}

first call to the api to get the data from db and store it in cache with key 55

first call 11ms

second call 5ms , as the data is fetched from cache


     */

    public Note getNoteById(Long noteId) {
        Optional<Note> note = noteRepository.findById(noteId);
        return note.orElse(null);
    }

   





    /*
     CachePut when creating it self we can use cacheput to store the data in cache

     */
  /*
    http://localhost:8080/api/notes
    {
    "title": "Sample Note",
    "content": "This is a sample note.",
    "addedDate": "2025-02-28T00:00:00.000+00:00",
    "isLive": true
}
    
OP

{
    "id": 102,
    "title": "Sample Note",
    "content": "This is a sample note.",
    "addedDate": "2025-02-28T00:00:00.000+00:00",
    "live": false
}
    136ms


    http://localhost:8080/api/notes/102
{
    "id": 102,
    "title": "Sample Note",
    "content": "This is a sample note.",
    "addedDate": "2025-02-28T00:00:00.000+00:00",
    "live": false
}
    6ms


    DB query was see when inserting the data in db but query was not seen when fetching the above data, as the data was fetched from cache
     */


     @CachePut(value = "note", key = "#note.id")
    public Note createNote(Note note) {
        return noteRepository.save(note);
    }

  







//     http://localhost:8080/api/notes/54


// {
//     "id": 54,
//     "title": "Sample Note updated",
//     "content": "This is a sample note.",
//     "addedDate": "2025-02-28T00:00:00.000+00:00",
//     "live": true
// }
//  post update the note with id 54 and the data is updated in db and cache as well and the data is fetched from cache 
//  tested the api
//  update action api -> service -> query to update the note with id 54 to db and update the cache for the note with id 54
//  next time when we call the api to get the note with id 54 , we get the data from cache


    public Note updateNote(Long id, Note noteDetails) {
        Optional<Note> optionalNote = noteRepository.findById(id);
        if (optionalNote.isPresent()) {
            Note note = optionalNote.get();
            note.setTitle(noteDetails.getTitle());
            note.setContent(noteDetails.getContent());
            note.setAddedDate(noteDetails.getAddedDate());
            note.setLive(noteDetails.isLive());

            Note updatedNote = noteRepository.save(note);

            // **Manually put updated note in cache**
            Cache cache = cacheManager.getCache("note");
            if (cache != null) {
                cache.put(id, updatedNote);
            }

            return updatedNote;
        } else {
            return null; // Note not found
        }
    }




    /*
     Deleted notes should not be present in cache



     http://localhost:8080/api/notes/102 
     we deleted the note with id 102 and removed from cache as well
api -> service -> query to delete the note with id 102 to db and remove the cache for the note with id 102

     when we call Get api for the note with id 102 , we get 404 not found from postman
     and when we checked in redis db we got null as the cache was removed for the note with id 102

     127.0.0.1:6379> GET note::102
"\xac\xed\x00\x05sr\x00+org.springframework.cache.support.NullValue\x00\x00\x00\x00\x00\x00\x00\x01\x02\x00\x00xp"

     */
    @CacheEvict(value = "note", key = "#id")
    public boolean deleteNote(Long id) {
        if (noteRepository.existsById(id)) {
            noteRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }

}