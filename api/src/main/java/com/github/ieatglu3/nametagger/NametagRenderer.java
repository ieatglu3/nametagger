package com.github.ieatglu3.nametagger;

/**
 * Responsible for rendering the tags of an entity for a viewer
 * All rendering methods are called on the platform thread executor
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
   * Initializes the tag renderer for a new entity. Called when a new entity is added to the viewer's tag list
   *
   * @param viewer  The {@link Viewer} who is viewing the tags
   * @param tags    The {@link AttachedTagList} containing the tags to be rendered for the viewing entity
   */
  public abstract void initialize(NametaggerPlatform platform, Viewer viewer, AttachedTagList tags);

  /**
   * Renders the tags for the source entity. Called every tick to update the tags
   *
   * @param viewer  The {@link Viewer} who is viewing the tags
   * @param viewMap    A map of entity IDs to their corresponding {@link AttachedTagList}s. The source entity's tag list can be accessed using the source entity's ID
   */
  public abstract void render(NametaggerPlatform platform, Viewer viewer, ViewMap viewMap);
}