This is an alternative implementation of the JDO attach/detach feature upon JPA.

**The problem:** There are many cases when you should transport your persistent entities between different contexts. For example: transport objects between requests in session, send/receive objects to/from a GWT/Flex/etc. application, etc.  In this cases the persistent object lose its attached state, which can cause many problems, LazyInitializatonExceptions on lazy property reads, or simple serialization problems. In JDO there is a sophisticated method to manually attach/detach the object from/to the persistent context. Detached objects are simple POJOs without lazy properties, and other ORM magics. You can transport this POJOs between contexts with the simple serialization/deserialization mechanisms. In JPA you cannot access this feature, you cannot manually detach a persistent object from the persistent context.

**The solution:** AttachDetachUtil gives this manual attach/detach feature to JPA (and possibly other ORM systems). When you call the detach() method, the utility makes a deep copy of the entity. This copy will contains the primitive attributes of the object. The complex properties like List-s, and objects will set to null in default case, but you can annotate this properties with @FetchOnDetach annotation which extend the detach mechanism to these. Detached objects are simple POJOs, they don't contain any lazy property, or other ORM dependent thing. It can be serialize, or store in HTTP session, and simply manipulate as other POJOs. After the data manipulation you can re-attach the objects with attach() method, which recursively walk on the tree, and merge back it to the database. On JPA, the utility uses Id attribute to recognize the state of object, and choose the merge or persist method for re-attachment.

Example:

```
@Entity
public class Entity {

  private Long id;
  private List<SubEntity> subEntities;

  @Id @GeneratedValue
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @OneToMany(mappedBy="entity")
  @FetchOnDetach
  public List<SubEntity> getSubEntities() {
    return subEntities;
  }

  public void setSubEntities(List<SubEntity> subEntities) {
    this.subEntities = subEntities;
  }
}
```

```
@Entity
public class SubEntity {

  private Long id;
  private Entity entity;

  @Id @GeneratedValue
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @ManyToOne
  @FetchOnDetach
  public Entity getEntity() {
    return entity;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }
}
```

```
Entity attachedEntity = entityManager.find(Entity.class, 1L);

// creating JPA based attach/detach util
JPAAttachDetachUtil adUtil= new JPAAttachDetachUtil(entityManager);

// detach entity
Entity detachedEntity = adUtil.detach(attachedEntity);

// do anything with the detached entity
SubEntity sub_entity = new SubEntity();
sub_entity.setEntity(detachedEntity);
sub_entity.setProperty("Something");

detachedEntity.getSubEntities().add(sub_entity);
...

// re-attach the entity to the EntityManager
adUtil.attach(detachedEntity);
```

<a href='https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=LMQGC6YTEQKE4&item_name=Beer'>
<img src='http://www.paypal.com/en_US/i/btn/x-click-but04.gif' /><br />Buy me some beer if you like my code ;)</a>

If you like the code, look at my other projects on http://code.google.com/u/TheBojda/.

If you have any question, please feel free to contact me at thebojda AT gmail DOT com.