package com.github.ieatglu3.nametagger;

/**
 * Responsible for rendering the tags of an entity for a viewer
 * All rendering methods are called on the platform thread
 */
public abstract class NametagRenderer
{

  private final String name;

  /**
   * Creates a new nametag renderer with the given name
   * @param name name of this renderer, used for debugging and logging purposes, doesn't have to be unique but should be descriptive
   */
  protected NametagRenderer(String name)
  {
    this.name = name;
  }

  /**
   * Gets the name of this renderer, used for debugging and logging purposes
   * @return name
   */
  public String name()
  {
    return this.name;
  }

  /**
   * Called when a viewer starts viewing an entity with this renderer
   *
   * @param viewer  The {@link Viewer} who is viewing the tags
   * @param taggedEntity    A {@link TaggedEntity} containing the tags currently attached to the entity for the viewer
   */
  public void startViewingEntity(NametaggerPlatform platform, Viewer viewer, TaggedEntity taggedEntity) {}

  /**
   * Called when a viewer stops viewing an entity with this renderer
   *
   * @param viewer  The {@link Viewer} who is viewing the tags
   * @param taggedEntity    A {@link TaggedEntity} containing the tags that were attached to the entity for the viewer before they stopped viewing it
   */
  public void stopViewingEntity(NametaggerPlatform platform, Viewer viewer, TaggedEntity taggedEntity) {}

  /**
   * Called when this renderer is attached to a viewer
   *
   * @param viewer  The {@link Viewer} who is viewing the tags
   * @param viewMap    A {@link ViewMap} containing the tags to be rendered for all entities visible to the viewer
   */
  public void attached(NametaggerPlatform platform, Viewer viewer, ViewMap viewMap) {}

  /**
   * Called when this renderer is detached from a viewer
   *
   * @param viewer  The {@link Viewer} who is viewing the tags
   * @param viewMap    A {@link ViewMap} containing the tags to be rendered for all entities visible to the viewer
   */
  public void detached(NametaggerPlatform platform, Viewer viewer, ViewMap viewMap) {}

  /**
   * Renders the tags for the source entity. Called every tick to update the tags
   *
   * @param viewer  The {@link Viewer} who is viewing the tags
   * @param viewMap    A {@link ViewMap} containing the tags to be rendered for all entities visible to the viewer
   */
  public abstract void render(NametaggerPlatform platform, Viewer viewer, ViewMap viewMap);
}